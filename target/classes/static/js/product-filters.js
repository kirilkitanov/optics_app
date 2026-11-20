function createProductFilters(config) {
    const products = config.products;
    const elements = config.elements;
    const onChange = config.onChange;

    const {
        productSearch,
        categoryFilter,
        brandFilter,
        modelFilter,
        sphFilter,
        cylFilter,
        addFilter,
        stockFilter
    } = elements;

    function normalizeText(value) {
        return String(value ?? '').trim().toLocaleLowerCase('bg-BG');
    }

    function getCategoryName(product) {
        return product.category && product.category.displayName
            ? product.category.displayName
            : String(product.category ?? '');
    }

    function numericMatches(value, filterValue) {
        const filter = String(filterValue ?? '').trim();

        if (!filter) {
            return true;
        }

        if (value === null || value === undefined || value === '') {
            return false;
        }

        const actualText = String(value).replace(',', '.').trim();
        const filterText = filter.replace(',', '.').trim();

        const actualNumber = Number(actualText);
        const filterNumber = Number(filterText);

        if (Number.isFinite(actualNumber) && Number.isFinite(filterNumber)) {
            return Math.abs(actualNumber - filterNumber) < 0.000001;
        }

        return normalizeText(actualText).includes(normalizeText(filterText));
    }

    function matchesProduct(product, excludedFilter = null) {
        const searchText = normalizeText(productSearch.value);

        const searchableValues = [
            getCategoryName(product),
            product.brand,
            product.model,
            product.barcode,
            product.sph,
            product.cyl,
            product.addPower,
            product.prism,
            product.sizeOrDiameter
        ];

        if (
            excludedFilter !== 'search' &&
            searchText &&
            !searchableValues.some(value =>
                normalizeText(value).includes(searchText)
            )
        ) {
            return false;
        }

        if (
            excludedFilter !== 'category' &&
            categoryFilter.value &&
            getCategoryName(product) !== categoryFilter.value
        ) {
            return false;
        }

        if (
            excludedFilter !== 'brand' &&
            brandFilter.value &&
            String(product.brand ?? '').trim() !== brandFilter.value
        ) {
            return false;
        }

        if (
            excludedFilter !== 'model' &&
            normalizeText(modelFilter.value) &&
            !normalizeText(product.model).includes(normalizeText(modelFilter.value))
        ) {
            return false;
        }

        if (
            excludedFilter !== 'sph' &&
            !numericMatches(product.sph, sphFilter.value)
        ) {
            return false;
        }

        if (
            excludedFilter !== 'cyl' &&
            !numericMatches(product.cyl, cylFilter.value)
        ) {
            return false;
        }

        if (
            excludedFilter !== 'add' &&
            !numericMatches(product.addPower, addFilter.value)
        ) {
            return false;
        }

        if (excludedFilter !== 'stock') {
            const quantity = Number(product.quantity ?? 0);

            if (stockFilter.value === 'non-zero' && quantity === 0) return false;
            if (stockFilter.value === 'in-stock' && quantity <= 0) return false;
            if (stockFilter.value === 'zero-stock' && quantity !== 0) return false;
            if (stockFilter.value === 'negative-stock' && quantity >= 0) return false;
        }

        return true;
    }

    function updateSelectOptions(element, values) {
        if (element.tagName !== 'SELECT') {
            return false;
        }

        const currentValue = element.value;

        const uniqueValues = [...new Set(values.filter(value => value !== ''))]
            .sort((a, b) => a.localeCompare(b, 'bg'));

        const validSelection = uniqueValues.includes(currentValue)
            ? currentValue
            : '';

        element.querySelectorAll('option:not([value=""])').forEach(option => {
            option.remove();
        });

        uniqueValues.forEach(value => {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = value;
            element.appendChild(option);
        });

        element.value = validSelection;

        return currentValue !== validSelection;
    }

    function updateDynamicFilters() {
        let changed = false;

        const filterDefinitions = [
            {
                key: 'category',
                element: categoryFilter,
                getValue: product => getCategoryName(product)
            },
            {
                key: 'brand',
                element: brandFilter,
                getValue: product => String(product.brand ?? '').trim()
            }
        ];

        filterDefinitions.forEach(definition => {
            const candidates = products.filter(product =>
                matchesProduct(product, definition.key)
            );

            const values = candidates
                .map(definition.getValue)
                .map(value => String(value ?? '').trim())
                .filter(Boolean);

            if (updateSelectOptions(definition.element, values)) {
                changed = true;
            }
        });

        return changed;
    }

    let syncingDynamicFilters = false;

    function refresh() {
        if (!syncingDynamicFilters) {
            syncingDynamicFilters = true;
            const changed = updateDynamicFilters();
            syncingDynamicFilters = false;

            if (changed) {
                refresh();
                return;
            }
        }

        const filteredProducts = products.filter(matchesProduct);

        if (typeof onChange === 'function') {
            onChange(filteredProducts);
        }

        return filteredProducts;
    }

    updateDynamicFilters();

    [
        productSearch,
        categoryFilter,
        brandFilter,
        modelFilter,
        sphFilter,
        cylFilter,
        addFilter
    ].forEach(element => {
        element.addEventListener('input', refresh);
        element.addEventListener('change', refresh);
    });

    stockFilter.addEventListener('change', refresh);

    refresh();

    return {
        matchesProduct: matchesProduct,
        getFilteredProducts: () => products.filter(matchesProduct),
        refresh: refresh
    };
}
