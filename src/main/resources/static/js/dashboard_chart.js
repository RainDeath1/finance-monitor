document.addEventListener('DOMContentLoaded', function () {

    const canvas = document.getElementById('monthlyChart');

    const chartData = window.financeChartData;

    if (!canvas || !chartData) {
        return;
    }

    if (typeof Chart === 'undefined') {
        console.error('Библиотека Chart.js не загружена');
        return;
    }

    new Chart(canvas, {

        type: 'bar',

        data: {
            labels: chartData.labels,

            datasets: [
                {
                    label: 'Доходы',
                    data: chartData.incomes,
                    backgroundColor: 'rgba(34, 197, 94, 0.7)'
                },
                {
                    label: 'Расходы',
                    data: chartData.expenses,
                    backgroundColor: 'rgba(239, 68, 68, 0.7)'
                }
            ]
        },

        options: {
            responsive: true,
            maintainAspectRatio: false,

            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });

});