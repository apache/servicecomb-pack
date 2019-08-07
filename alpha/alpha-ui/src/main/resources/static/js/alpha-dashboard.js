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
  $.ajax('/ui/transaction/statistics', {
    success: function (data) {
      $('#statistics-total').text(data.total)
      $('#statistics-successful').text(data.successful)
      $('#statistics-compensated').text(data.compensated)
      $('#statistics-failed').text(data.failed)
    },
    error: function (state) {
      // TODO show message
    }
  });

  $.ajax('/ui/transaction/slow', {
    success: function (data) {
      for (i = 0; i < data.length; i++) {
        $('.slow-topn').append(
            '<a href="/ui/transaction/' + data[i].globalTxId
            + '"><div class="progress mb-3" id="slow-top-"' + i + '>\n'
            + '<div class="progress-bar" role="progressbar" style="cursor:pointer; width: '
            + (data[i].durationTime / data[0].durationTime) * 100
            + '%" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100">'
            + data[i].durationTime + ' ms</div>\n'
            + '</div></a>')
      }
    },
    error: function (state) {
      // TODO show message
    }
  });


});