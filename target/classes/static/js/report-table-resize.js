document.addEventListener('DOMContentLoaded', function () {
    const tables = document.querySelectorAll('.report-results-table');

    tables.forEach((table, tableIndex) => {
        const headerRow = table.querySelector('thead tr');
        if (!headerRow) return;

        const headers = Array.from(headerRow.children);
        if (!headers.length) return;

        const storageKey = 'optics-report-column-widths:' +
            headers.map(header => header.textContent.trim()).join('|');

        let savedWidths = {};
        try {
            savedWidths = JSON.parse(localStorage.getItem(storageKey) || '{}');
        } catch (error) {
            savedWidths = {};
        }

        let colgroup = table.querySelector('colgroup');
        if (!colgroup) {
            colgroup = document.createElement('colgroup');
            table.insertBefore(colgroup, table.firstChild);
        }
        colgroup.replaceChildren();

        const columns = headers.map((header, index) => {
            const column = document.createElement('col');
            const defaultWidth = Math.max(54, Math.min(230, header.scrollWidth + 18));
            const width = Number(savedWidths[index]) || defaultWidth;
            column.style.width = width + 'px';
            colgroup.appendChild(column);

            header.title = header.textContent.trim();

            const handle = document.createElement('span');
            handle.className = 'report-column-resizer';
            handle.setAttribute('role', 'separator');
            handle.setAttribute('aria-orientation', 'vertical');
            handle.setAttribute('aria-label', 'Промени ширината на колоната ' + header.textContent.trim());
            header.appendChild(handle);

            handle.addEventListener('pointerdown', function (event) {
                event.preventDefault();
                event.stopPropagation();

                const startX = event.clientX;
                const startWidth = column.getBoundingClientRect().width;
                const startTableWidth = table.getBoundingClientRect().width;
                handle.classList.add('is-resizing');
                handle.setPointerCapture(event.pointerId);

                function onMove(moveEvent) {
                    const newWidth = Math.max(48, Math.round(startWidth + moveEvent.clientX - startX));
                    column.style.width = newWidth + 'px';
                    table.style.width = Math.max(
                        table.parentElement.clientWidth,
                        startTableWidth + newWidth - startWidth
                    ) + 'px';
                }

                function onEnd() {
                    handle.classList.remove('is-resizing');
                    const widths = Array.from(colgroup.children).map(col =>
                        Math.round(col.getBoundingClientRect().width)
                    );
                    try {
                        localStorage.setItem(storageKey, JSON.stringify(widths));
                    } catch (error) {
                        // Column resizing still works for the current page if storage is unavailable.
                    }
                    handle.removeEventListener('pointermove', onMove);
                    handle.removeEventListener('pointerup', onEnd);
                    handle.removeEventListener('pointercancel', onEnd);
                }

                handle.addEventListener('pointermove', onMove);
                handle.addEventListener('pointerup', onEnd);
                handle.addEventListener('pointercancel', onEnd);
            });

            return column;
        });

        table.querySelectorAll('tbody tr').forEach(row => {
            Array.from(row.children).forEach(cell => {
                if (cell.textContent.trim()) cell.title = cell.textContent.trim();
            });
        });

        const totalWidth = columns.reduce((sum, column) =>
            sum + (Number.parseFloat(column.style.width) || 0), 0);
        table.style.width = Math.max(table.parentElement.clientWidth, totalWidth) + 'px';
    });
});
