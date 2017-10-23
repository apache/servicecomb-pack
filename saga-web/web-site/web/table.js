$("#content").bootstrapTable({
    method: 'get',
    dataType: 'json',
    url: 'data2.json',
    cache: false,
    totalField: 'pageSize',
    dataField: 'requests',
    pageNumber: 1,
    pageSize: 10,
    pagination: true,
    sidePagination: "server",

    columns: [
        {checkbox: true},
        {
            field: 'id',
            title: 'Id',
            align: 'center',
            width: '10%'
        },
        {
            field: 'sagaId',
            title: 'SagaId',
            align: 'center',
            width: '35%'
        },
        {
            field: 'startTime',
            title: 'StartTime',
            align: 'center',
            width: '20%',
            //——修改——获取日期列的值进行转换
            formatter: function (value, row, index) {
                return changeDateFormat(value);
            }
        },
        {
            field: 'completedTime',
            title: 'CompletedTime',
            align: 'center',
            width: '20%',
            //——修改——获取日期列的值进行转换
            formatter: function (value, row, index) {
                return changeDateFormat(value);
            }
        },
        {
            field: 'status',
            title: 'Status',
            align: 'center',
            width: '15%'
        }

    ]

});

function changeDateFormat(value) {
    var date = new Date(value);
    var y = date.getFullYear();
    var m = date.getMonth() + 1;
    m = m < 10 ? ('0' + m) : m;
    var d = date.getDate();
    d = d < 10 ? ('0' + d) : d;
    var h = date.getHours();
    var minute = date.getMinutes();
    minute = minute < 10 ? ('0' + minute) : minute;
    var second = date.getSeconds();
    second = second < 10 ? ('0' + second) : second;
    return y + '-' + m + '-' + d + ' ' + h + ':' + minute + ':' + second;
}
