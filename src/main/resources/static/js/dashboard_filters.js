document.addEventListener('DOMContentLoaded', function () {

    const periodSelect = document.getElementById('period');

    const monthField = document.getElementById('month-field');

    const quarterField = document.getElementById('quarter-field');

    if (!periodSelect || !monthField || !quarterField) {
        return;
    }

function updateFields() {
    const period = periodSelect.value;

    monthField.hidden = period !== 'month';
    quarterField.hidden = period !== 'quarter';

    document.getElementById('month').disabled = period !== 'month';
    document.getElementById('quarter').disabled = period !== 'quarter';
}

    periodSelect.addEventListener('change', updateFields);

    updateFields();

});
