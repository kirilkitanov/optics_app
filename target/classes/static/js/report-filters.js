
document.addEventListener('DOMContentLoaded', function () {
    const productSelect = document.getElementById('reportProductSelect');

    if (!productSelect) {
        return;
    }

    const form = productSelect.closest('form');
    const options = Array.from(productSelect.options)
        .filter(option => option.value !== '');

    const selected = new Map();

    options.forEach(option => {
        if (option.selected) {
            selected.set(option.value, option.textContent.trim());
        }
    });

    // Оригиналният select остава само като източник на продуктите.
    productSelect.removeAttribute('name');
    productSelect.hidden = true;

    const wrapper = document.createElement('div');
    wrapper.className = 'report-product-picker';

    const search = document.createElement('input');
    search.type = 'text';
    search.className = 'report-product-search';
    search.placeholder = 'Търси по бранд, модел, баркод или диоптър...';
    search.autocomplete = 'off';

    const suggestions = document.createElement('div');
    suggestions.className = 'report-product-suggestions';
    suggestions.hidden = true;

    const selectedList = document.createElement('div');
    selectedList.className = 'report-selected-products';

    const hiddenInputs = document.createElement('div');
    hiddenInputs.className = 'report-product-hidden-inputs';

    productSelect.parentElement.insertBefore(wrapper, productSelect);
    wrapper.append(search, suggestions, selectedList, hiddenInputs);

    function renderSelected() {
        selectedList.replaceChildren();
        hiddenInputs.replaceChildren();

        selected.forEach((label, id) => {
            const item = document.createElement('div');
            item.className = 'report-selected-product';

            const name = document.createElement('span');
            name.textContent = label;

            const remove = document.createElement('button');
            remove.type = 'button';
            remove.textContent = '×';
            remove.setAttribute('aria-label', 'Премахни продукт');

            remove.addEventListener('click', function () {
                selected.delete(id);
                renderSelected();
                renderSuggestions();
            });

            item.append(name, remove);
            selectedList.appendChild(item);

            const hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = 'productIds';
            hidden.value = id;
            hiddenInputs.appendChild(hidden);
        });
    }

    function renderSuggestions() {
        suggestions.replaceChildren();

        const query = search.value.trim().toLocaleLowerCase('bg-BG');

        if (query.length < 1) {
            suggestions.hidden = true;
            return;
        }

        const matches = options.filter(option => {
            const label = option.textContent.trim();
            return !selected.has(option.value)
                && label.toLocaleLowerCase('bg-BG').includes(query);
        }).slice(0, 30);

        if (matches.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'report-product-no-results';
            empty.textContent = 'Няма намерени продукти.';
            suggestions.appendChild(empty);
        } else {
            matches.forEach(option => {
                const item = document.createElement('button');
                item.type = 'button';
                item.className = 'report-product-suggestion';
                item.textContent = option.textContent.trim();

                item.addEventListener('click', function () {
                    selected.set(option.value, option.textContent.trim());
                    search.value = '';
                    renderSelected();
                    renderSuggestions();
                    search.focus();
                });

                suggestions.appendChild(item);
            });
        }

        suggestions.hidden = false;
    }

    search.addEventListener('input', renderSuggestions);

    search.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            suggestions.hidden = true;
        }
    });

    document.addEventListener('click', function (event) {
        if (!wrapper.contains(event.target)) {
            suggestions.hidden = true;
        }
    });

    renderSelected();

    form.addEventListener('submit', function () {
        // Скритите полета productIds се изпращат заедно с формата.
    });
});
