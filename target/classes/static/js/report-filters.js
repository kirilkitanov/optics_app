
document.addEventListener('DOMContentLoaded', function () {
    const productSelect = document.getElementById('reportProductSelect');

    if (!productSelect) {
        return;
    }

    const options = Array.from(productSelect.options)
        .filter(option => option.value !== '');

    const search = document.createElement('input');
    search.type = 'text';
    search.placeholder = 'Търси продукт по бранд, модел или баркод...';
    search.className = 'report-product-search';
    search.autocomplete = 'off';

    productSelect.parentElement.insertBefore(search, productSelect);

    search.addEventListener('input', function () {
        const query = search.value.trim().toLocaleLowerCase('bg-BG');

        options.forEach(option => {
            const matches = option.textContent
                .toLocaleLowerCase('bg-BG')
                .includes(query);

            option.hidden = !matches;
        });
    });
});
