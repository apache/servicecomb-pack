/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

$(document).ready(function () {

  function datatablesRequest(data) {
    for (var i = 0; i < data.columns.length; i++) {
      column = data.columns[i];
      column.searchRegex = column.search.regex;
      column.searchValue = column.search.value;
      delete(column.search);
    }
  }

  var transaction_table = $('#dataTable').DataTable({
    pagingType: "simple_numbers",
    info: true,
    filter: false,
    lengthMenu: [[10, 25, 50, -1], [10, 25, 50, "All"]],
    processing: true,
    serverSide: true,
    order: [[ 4, "desc" ]],
    ajax: {
      url : $('#transaction_config').attr('ajax'),
      type: 'POST',
      data: function(data) {
        datatablesRequest(data);
      }
    },
    language: {
      lengthMenu: "_MENU_",
    },
    columns: [
      { "data": "serviceName" },
      { "data": "instanceId" },
      { "data": "globalTxId" },
      { "data": "subTxSize" },
      { "data": "beginTime" },
      { "data": "durationTime" },
      { "data": "state" },
      { "data": "" }
    ],
    columnDefs: [
      {
        render: function (data, type, row) {
          return '<i class="fas fa-fw fa-bullseye row-transaction" style="cursor:pointer" globalTxId='+row.globalTxId+'></i>';
        },
        width: "50px",
        targets: -1
      },
      {
        render: function (data, type, row) {
          if(data == 'COMMITTED'){
            return '<span class="text-success">'+data+'</span>'
          }else if(data == 'SUSPENDED'){
            return '<span class="text-danger">'+data+'</span>'
          }else if(data == 'COMPENSATED'){
            return '<span class="text-warning">'+data+'</span>'
          }else{
            return data;
          }
        },
        width: "50px",
        targets: 6
      },
      { "visible": false,  "targets": [ 4 ] }
    ]
  });

  $('#dataTable tbody').on("click","tr", function(_event){
    var data = transaction_table.row( this ).data();
    window.location = "/ui/transaction/"+data.globalTxId
  });

  // table toolbar add state select & custom layout
  $('#dataTable_wrapper .row:first div:first').removeClass("col-sm-12 col-md-6");
  $('#dataTable_wrapper .row:first div:last').removeClass("col-sm-12 col-md-6");
  $('#dataTable_wrapper .row:first div:first').addClass("col-sm-18 col-md-9");
  $('#dataTable_wrapper .row:first div:last').addClass("col-sm-6 col-md-3");
  $('#dataTable_wrapper .row:first div:last').append('<select name="state_select" class="custom-select custom-select-sm form-control form-control-sm"><option value="ALL">ALL</option><option value="COMMITTED">COMMITTED</option><option value="COMPENSATED">COMPENSATED</option><option value="SUSPENDED">SUSPENDED</option></select>')
});