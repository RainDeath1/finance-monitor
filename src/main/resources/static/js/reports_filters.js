document.addEventListener('DOMContentLoaded', function () {

    const periodSelect = document.getElementById('period');

    if (!periodSelect) {
        return;
    }

    const fields = {
        year: document.getElementById('report-year-field'),
        month: document.getElementById('report-month-field'),
        quarter: document.getElementById('report-quarter-field'),
        day: document.getElementById('report-day-field'),
        dateFrom: document.getElementById('report-date-from-field'),
        dateTo: document.getElementById('report-date-to-field')
    };

    function updateFields() {

        const period = periodSelect.value;

        const visible = {
            year: ['month', 'quarter', 'year'].includes(period),
            month: period === 'month',
            quarter: period === 'quarter',
            day: period === 'day',
            dateFrom: period === 'custom',
            dateTo: period === 'custom'
        };

        for (const [name, field] of Object.entries(fields)) {

            if (!field) {
                continue;
            }

            field.hidden = !visible[name];

            field.querySelectorAll('input, select').forEach(input => {
                input.disabled = !visible[name];
            });
        }
    }

    periodSelect.addEventListener('change', updateFields);

    updateFields();

});