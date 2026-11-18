document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('.report-filters');
    const categorySelect = form?.querySelector('[name="categories"]');
    const brandSelect = document.getElementById('reportBrandSelect');
    const modelSelect = document.getElementById('reportModelSelect');
    const productSelect = document.getElementById('reportProductSelect');
    const search = document.getElementById('reportProductSearch');
    const suggestions = document.getElementById('reportProductSuggestions');
    const selectedList = document.getElementById('reportSelectedProducts');
    const hiddenInputs = document.getElementById('reportProductHiddenInputs');

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
        const query = normalize(search.value);

        if (!query) {
            suggestions.hidden = true;
            return;
        }

        const matches = products.filter(product => {
            if (selected.has(product.id) || !categoryMatches(product)) return false;
            if (brandSelect.value && product.brand !== brandSelect.value) return false;
            if (modelSelect.value && product.model !== modelSelect.value) return false;
            return normalize(product.label).includes(query);
        }).slice(0, 40);

        if (matches.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'report-product-no-results';
            empty.textContent = 'Няма намерени продукти с тази стойност.';
            suggestions.appendChild(empty);
        } else {
            matches.forEach(product => {
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'report-product-suggestion';
                button.textContent = product.label;
                button.addEventListener('click', () => {
                    selected.set(product.id, product);
                    search.value = '';
                    suggestions.hidden = true;
                    renderSelected();
                    search.focus();
                });
                suggestions.appendChild(button);
            });
        }

        suggestions.hidden = false;
    }

    categorySelect?.addEventListener('change', () => {
        selected.forEach((product, id) => {
            if (!categoryMatches(product)) selected.delete(id);
        });
        refreshBrands('');
        renderSelected();
        renderSuggestions();
    });

    brandSelect.addEventListener('change', () => {
        refreshModels('');
        renderSuggestions();
    });

    modelSelect.addEventListener('change', renderSuggestions);
    search.addEventListener('input', renderSuggestions);
    search.addEventListener('keydown', event => {
        if (event.key === 'Escape') suggestions.hidden = true;
    });

    document.addEventListener('click', event => {
        if (!event.target.closest('#reportProductPicker')) suggestions.hidden = true;
    });

    refreshBrands(originalBrand);
    refreshModels(originalModel);
    renderSelected();
});