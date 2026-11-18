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

    let products = Array.from(productSelect.options).map(option => ({
        id: option.value, label: option.textContent.trim(),
        category: option.dataset.category || '', brand: option.dataset.brand || '',
        model: option.dataset.model || '', selected: option.selected
    })).filter(product => product.id);
    const searchUrl = search.dataset.searchUrl;
    const filterUrl = search.dataset.filterUrl;
    let filterOptions = [];
    let searchTimer;
    let requestSequence = 0;

    // Изборът се изпраща чрез hidden inputs, за да няма дублиране от скрития select.
    productSelect.removeAttribute('name');

    const selected = new Map(products.filter(p => p.selected).map(p => [p.id, p]));
    const originalBrand = brandSelect.dataset.selectedBrand || brandSelect.value;
    const originalModel = modelSelect.dataset.selectedModel || modelSelect.value;

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
        const categories = getCategories();
        const available = filterOptions.filter(item => !categories.length || categories.includes(item.category));
        const brands = [...new Set(available.map(item => item.brand).filter(Boolean))].sort((a,b)=>a.localeCompare(b,'bg'));
        fillSelect(brandSelect, brands, 'Всички брандове', preferredBrand);
        if (!brands.includes(brandSelect.value)) brandSelect.value = '';
        refreshModels(modelSelect.value);
    }

    function refreshModels(preferredModel = modelSelect.value) {
        const categories = getCategories();
        const available = filterOptions.filter(item =>
            (!categories.length || categories.includes(item.category)) &&
            (!brandSelect.value || item.brand === brandSelect.value));
        const models = [...new Set(available.map(item=>item.model).filter(Boolean))].sort((a,b)=>a.localeCompare(b,'bg'));
        fillSelect(modelSelect, models, 'Всички модели', preferredModel);
        modelSelect.disabled = models.length === 0;
    }

    async function loadFilterOptions() {
        const response = await fetch(filterUrl);
        if (!response.ok) throw new Error('Неуспешно зареждане на продуктовите филтри.');
        filterOptions = await response.json();
        refreshBrands(originalBrand);
        refreshModels(originalModel);
    }

    let nextOffset = 0;
    let hasMoreMatches = false;
    let loadingMoreMatches = false;
    let activeSearchKey = '';

    function buildSearchKey() {
        return JSON.stringify({
            query: normalize(search.value),
            categories: getCategories(),
            brand: brandSelect.value,
            model: modelSelect.value
        });
    }

    async function fetchProductMatches(reset = false) {
        const query = normalize(search.value);
        if (!query) {
            requestSequence++;
            currentMatches = [];
            nextOffset = 0;
            hasMoreMatches = false;
            loadingMoreMatches = false;
            activeSearchKey = '';
            suggestions.replaceChildren();
            suggestions.hidden = true;
            return;
        }

        const searchKey = buildSearchKey();
        if (reset) {
            requestSequence++;
            currentMatches = [];
            nextOffset = 0;
            hasMoreMatches = true;
            activeSearchKey = searchKey;
            suggestions.scrollTop = 0;
        } else if (loadingMoreMatches || !hasMoreMatches || searchKey !== activeSearchKey) {
            return;
        }

        const sequence = requestSequence;
        loadingMoreMatches = true;
        const params = new URLSearchParams({ query, offset: String(nextOffset) });
        getCategories().forEach(category => params.append('categories', category));
        if (brandSelect.value) params.set('brand', brandSelect.value);
        if (modelSelect.value) params.set('model', modelSelect.value);

        try {
            const response = await fetch(searchUrl + '?' + params.toString());
            if (!response.ok) throw new Error('Search failed');
            const matches = await response.json();
            if (sequence !== requestSequence || searchKey !== buildSearchKey()) return;

            currentMatches = reset ? matches : currentMatches.concat(matches);
            nextOffset += matches.length;
            hasMoreMatches = matches.length === 40;
            renderSuggestionsFromMatches();
        } catch (error) {
            if (sequence === requestSequence) {
                suggestions.replaceChildren();
                const message = document.createElement('div');
                message.className = 'report-product-no-results';
                message.textContent = 'Търсенето временно не е достъпно.';
                suggestions.appendChild(message);
                suggestions.hidden = false;
            }
        } finally {
            if (sequence === requestSequence) {
                loadingMoreMatches = false;

                // If the first batch does not fill the visible list, load the next batch automatically.
                if (hasMoreMatches && !suggestions.hidden
                        && suggestions.scrollHeight <= suggestions.clientHeight + 2) {
                    fetchProductMatches(false);
                }
            }
        }
    }

    function renderSuggestions() {
        clearTimeout(searchTimer);
        requestSequence++;
        loadingMoreMatches = false;
        searchTimer = setTimeout(() => fetchProductMatches(true), 250);
    }

    function renderSuggestionsFromMatches() {
        const previousScrollTop = suggestions.scrollTop;
        suggestions.replaceChildren();
        activeSuggestionIndex = -1;
        if (!currentMatches.length) {
            const empty = document.createElement('div');
            empty.className = 'report-product-no-results';
            empty.textContent = 'Няма намерени продукти с тази стойност.';
            suggestions.appendChild(empty);
        } else {
            currentMatches.forEach((item,index) => {
                const product = {id:String(item.id), label:item.label, category:item.category, brand:item.brand, model:item.model};
                products = products.filter(existing => existing.id !== product.id);
                products.push(product);
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'report-product-suggestion' + (selected.has(product.id) ? ' is-selected' : '');
                button.textContent = product.label;
                button.addEventListener('mousedown', event => event.preventDefault());
                button.addEventListener('mouseenter', () => setActiveSuggestion(index));
                button.addEventListener('click', event => {
                    if (event.shiftKey && lastClickedSuggestionIndex >= 0) {
                        const from=Math.min(lastClickedSuggestionIndex,index), to=Math.max(lastClickedSuggestionIndex,index);
                        for(let i=from;i<=to;i++){const match=currentMatches[i];const p={id:String(match.id),label:match.label,category:match.category,brand:match.brand,model:match.model};selected.set(p.id,p);}
                    } else if(event.ctrlKey || event.metaKey) {
                        if(selected.has(product.id)) selected.delete(product.id); else selected.set(product.id,product);
                    } else selected.set(product.id,product);
                    lastClickedSuggestionIndex=index;
                    renderSelected();
                    renderSuggestionsFromMatches();
                    search.focus();
                });
                suggestions.appendChild(button);
            });
        }
        suggestions.hidden = false;
        suggestions.scrollTop = previousScrollTop;
    }

    suggestions.addEventListener('scroll', () => {
        if (!suggestions.hidden && hasMoreMatches && !loadingMoreMatches
                && suggestions.scrollTop + suggestions.clientHeight >= suggestions.scrollHeight - 45) {
            fetchProductMatches(false);
        }
    });

    function setActiveSuggestion(index) {
        const items = Array.from(suggestions.querySelectorAll('.report-product-suggestion'));
        if (!items.length) return;
        activeSuggestionIndex = Math.max(0,Math.min(index,items.length-1));
        items.forEach((item,i)=>item.classList.toggle('is-active',i===activeSuggestionIndex));
        items[activeSuggestionIndex].scrollIntoView({block:'nearest'});
    }

    function chooseActiveSuggestion() {
        const item=suggestions.querySelectorAll('.report-product-suggestion')[activeSuggestionIndex];
        if(item) item.click();
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
    loadFilterOptions().catch(error => console.error(error));
    renderSelected();
});