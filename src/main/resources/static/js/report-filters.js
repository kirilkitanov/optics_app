document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('.report-filters');
    const categorySelect = document.getElementById('reportCategorySelect');
    const categoryPicker = document.getElementById('reportCategoryPicker');
    const categoryTrigger = document.getElementById('reportCategoryTrigger');
    const categoryOptions = document.getElementById('reportCategoryOptions');
    const brandSelect = document.getElementById('reportBrandSelect');
    const modelSelect = document.getElementById('reportModelSelect');
    const productSelect = document.getElementById('reportProductSelect');
    const search = document.getElementById('reportProductSearch');
    const suggestions = document.getElementById('reportProductSuggestions');
    const selectedList = document.getElementById('reportSelectedProducts');
    const hiddenInputs = document.getElementById('reportProductHiddenInputs');
    const paymentSelect = document.getElementById('reportPaymentSelect');
    const paymentTrigger = document.getElementById('reportPaymentTrigger');
    const paymentOptions = document.getElementById('reportPaymentOptions');
    let activeSuggestionIndex = -1;
    let currentMatches = [];
    let lastClickedSuggestionIndex = -1;

    if (!form || !productSelect || !brandSelect || !modelSelect || !search) return;

    const products = Array.from(productSelect.options).map(option => ({
        id: option.value,
        label: option.textContent.trim(),
        category: option.dataset.category || '',
        brand: option.dataset.brand || '',
        model: option.dataset.model || '',
        selected: option.selected
    })).filter(product => product.id);

    // Изборът се изпраща чрез hidden inputs, за да няма дублиране от скрития select.
    productSelect.removeAttribute('name');

    const selected = new Map(products.filter(p => p.selected).map(p => [p.id, p]));
    const originalBrand = brandSelect.value;
    const originalModel = modelSelect.value;

    function normalize(value) {
        return String(value || '')
            .toLocaleLowerCase('bg-BG')
            .replace(/,/g, '.')
            .replace(/\s+/g, ' ')
            .trim();
    }

    function updatePaymentPicker() {
        if (!paymentSelect || !paymentTrigger || !paymentOptions) return;
        const selectedValues = new Set(Array.from(paymentSelect.selectedOptions).map(option => option.value));
        paymentOptions.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
            checkbox.checked = selectedValues.has(checkbox.value);
        });
        const labels = Array.from(paymentSelect.selectedOptions).map(option => option.textContent);
        paymentTrigger.textContent = labels.length ? labels.join(', ') : 'Всички методи';
    }

    paymentTrigger?.addEventListener('click', () => {
        paymentOptions.hidden = !paymentOptions.hidden;
    });

    paymentOptions?.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
        checkbox.addEventListener('change', () => {
            const option = Array.from(paymentSelect.options).find(item => item.value === checkbox.value);
            if (option) option.selected = checkbox.checked;
            updatePaymentPicker();
        });
    });

    document.addEventListener('click', event => {
        if (!event.target.closest('#reportPaymentPicker') && paymentOptions) {
            paymentOptions.hidden = true;
        }
    });

    function updateCategoryPicker() {
        if (!categorySelect || !categoryOptions || !categoryTrigger) return;
        const selectedValues = new Set(Array.from(categorySelect.selectedOptions).map(option => option.value));
        categoryOptions.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
            checkbox.checked = selectedValues.has(checkbox.value);
        });
        const labels = Array.from(categorySelect.selectedOptions).map(option => option.textContent);
        categoryTrigger.textContent = labels.length ? labels.join(', ') : 'Всички категории';
    }

    categoryTrigger?.addEventListener('click', () => {
        categoryOptions.hidden = !categoryOptions.hidden;
    });

    categoryOptions?.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
        checkbox.addEventListener('change', () => {
            const option = Array.from(categorySelect.options).find(item => item.value === checkbox.value);
            if (option) option.selected = checkbox.checked;
            categorySelect.dispatchEvent(new Event('change', { bubbles: true }));
            updateCategoryPicker();
        });
    });

    document.addEventListener('click', event => {
        if (!event.target.closest('#reportCategoryPicker') && categoryOptions) {
            categoryOptions.hidden = true;
        }
    });

    function getCategories() {
        return categorySelect
            ? Array.from(categorySelect.selectedOptions).map(option => option.value)
            : [];
    }

    function categoryMatches(product) {
        const categories = getCategories();
        return categories.length === 0 || categories.includes(product.category);
    }

    function fillSelect(select, values, placeholder, selectedValue) {
        select.replaceChildren();
        const first = document.createElement('option');
        first.value = '';
        first.textContent = placeholder;
        select.appendChild(first);

        values.forEach(value => {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = value;
            select.appendChild(option);
        });

        if (selectedValue && values.includes(selectedValue)) select.value = selectedValue;
    }

    function refreshBrands(preferredBrand = brandSelect.value) {
        const available = products.filter(categoryMatches);
        const brands = [...new Set(available.map(p => p.brand).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, 'bg'));
        fillSelect(brandSelect, brands, 'Всички брандове', preferredBrand);

        if (!brands.includes(brandSelect.value)) brandSelect.value = '';
        refreshModels(modelSelect.value);
    }

    function refreshModels(preferredModel = modelSelect.value) {
        const brand = brandSelect.value;
        const available = products.filter(p => categoryMatches(p) && (!brand || p.brand === brand));
        const models = [...new Set(available.map(p => p.model).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, 'bg'));

        fillSelect(modelSelect, models, 'Всички модели', preferredModel);
        modelSelect.disabled = models.length === 0;
    }

    function renderSelected() {
        selectedList.replaceChildren();
        hiddenInputs.replaceChildren();

        selected.forEach(product => {
            const chip = document.createElement('div');
            chip.className = 'report-selected-product';

            const label = document.createElement('span');
            label.textContent = product.label;

            const remove = document.createElement('button');
            remove.type = 'button';
            remove.textContent = '×';
            remove.setAttribute('aria-label', 'Премахни продукт');
            remove.addEventListener('click', () => {
                selected.delete(product.id);
                renderSelected();
            });

            chip.append(label, remove);
            selectedList.appendChild(chip);

            const hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = 'productIds';
            hidden.value = product.id;
            hiddenInputs.appendChild(hidden);
        });
    }

    function renderSuggestions() {
        suggestions.replaceChildren();
        activeSuggestionIndex = -1;
        const query = normalize(search.value);

        if (!query) {
            currentMatches = [];
            suggestions.hidden = true;
            return;
        }

        currentMatches = products.filter(product => {
            if (!categoryMatches(product)) return false;
            if (brandSelect.value && product.brand !== brandSelect.value) return false;
            if (modelSelect.value && product.model !== modelSelect.value) return false;
            return normalize(product.label).includes(query);
        }).slice(0, 40);

        if (currentMatches.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'report-product-no-results';
            empty.textContent = 'Няма намерени продукти с тази стойност.';
            suggestions.appendChild(empty);
        } else {
            currentMatches.forEach((product, index) => {
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'report-product-suggestion';
                button.textContent = product.label;
                button.setAttribute('aria-pressed', String(selected.has(product.id)));

                if (selected.has(product.id)) {
                    button.classList.add('is-selected');
                }

                // Keep the search input focused so the results remain open while selecting.
                button.addEventListener('mousedown', event => event.preventDefault());
                button.addEventListener('mouseenter', () => setActiveSuggestion(index));
                button.addEventListener('click', event => {
                    if (event.shiftKey && lastClickedSuggestionIndex >= 0) {
                        const start = Math.min(lastClickedSuggestionIndex, index);
                        const end = Math.max(lastClickedSuggestionIndex, index);

                        for (let i = start; i <= end; i++) {
                            selected.set(currentMatches[i].id, currentMatches[i]);
                        }
                    } else if (event.ctrlKey || event.metaKey) {
                        if (selected.has(product.id)) {
                            selected.delete(product.id);
                        } else {
                            selected.set(product.id, product);
                        }
                        lastClickedSuggestionIndex = index;
                    } else {
                        selected.set(product.id, product);
                        lastClickedSuggestionIndex = index;
                    }

                    renderSelected();
                    renderSuggestions();
                    search.focus();
                });
                suggestions.appendChild(button);
            });
        }

        suggestions.hidden = false;
    }

    categorySelect?.addEventListener('change', () => {
        lastClickedSuggestionIndex = -1;
        selected.forEach((product, id) => {
            if (!categoryMatches(product)) selected.delete(id);
        });
        refreshBrands('');
        renderSelected();
        renderSuggestions();
    });

    brandSelect.addEventListener('change', () => {
        lastClickedSuggestionIndex = -1;
        refreshModels('');
        renderSuggestions();
    });

    modelSelect.addEventListener('change', () => {
        lastClickedSuggestionIndex = -1;
        renderSuggestions();
    });
    search.addEventListener('input', () => {
        lastClickedSuggestionIndex = -1;
        renderSuggestions();
    });
    search.addEventListener('keydown', event => {
        const items = suggestions.querySelectorAll('.report-product-suggestion');
        if (event.key === 'ArrowDown' && !suggestions.hidden && items.length) {
            event.preventDefault();
            setActiveSuggestion(activeSuggestionIndex + 1);
        } else if (event.key === 'ArrowUp' && !suggestions.hidden && items.length) {
            event.preventDefault();
            setActiveSuggestion(activeSuggestionIndex < 0 ? items.length - 1 : activeSuggestionIndex - 1);
        } else if (event.key === 'Enter' && !suggestions.hidden && items.length) {
            event.preventDefault();
            if (activeSuggestionIndex < 0) setActiveSuggestion(0);
            chooseActiveSuggestion();
        } else if (event.key === 'Escape') {
            suggestions.hidden = true;
        }
    });

    document.addEventListener('click', event => {
        if (!event.target.closest('#reportProductPicker')) suggestions.hidden = true;
    });

    updateCategoryPicker();
    refreshBrands(originalBrand);
    refreshModels(originalModel);
    renderSelected();
});