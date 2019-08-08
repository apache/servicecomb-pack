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
      $('#statistics-total').text(digitUnit(data.total,0));
      $('#statistics-successful').text(digitUnit(data.successful,0));
      $('#statistics-compensated').text(digitUnit(data.compensated,0));
      $('#statistics-failed').text(digitUnit(data.failed,0));
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

  $.ajax('/ui/transaction/metrics', {
    success: function (data) {
      //events
      $('#metrics-events-received').text(digitUnit(data.metrics.eventReceived,0));
      $('#metrics-events-accepted').text(digitUnit(data.metrics.eventAccepted,0));
      $('#metrics-events-rejected').text(digitUnit(data.metrics.eventRejected,0));
      $('#metrics-events-average-time').text(data.metrics.eventAvgTime+' ms / event');
      $('#metrics-events-received-progress').css('width',data.metrics.eventReceived==0?'0%':'100%');
      $('#metrics-events-accepted-progress').css('width',(data.metrics.eventAccepted/data.metrics.eventReceived)*100+'%');
      $('#metrics-events-rejected-progress').css('width',(data.metrics.eventRejected/data.metrics.eventReceived)*100+'%');
      //actors
      $('#metrics-actors-received').text(digitUnit(data.metrics.actorReceived,0));
      $('#metrics-actors-accepted').text(digitUnit(data.metrics.actorAccepted,0));
      $('#metrics-actors-rejected').text(digitUnit(data.metrics.actorRejected,0));
      $('#metrics-actors-average-time').text(data.metrics.actorAvgTime+' ms / event');
      $('#metrics-actors-received-progress').css('width',data.metrics.actorReceived==0?'0%':'100%');
      $('#metrics-actors-accepted-progress').css('width',(data.metrics.actorAccepted/data.metrics.actorReceived)*100+'%');
      $('#metrics-actors-rejected-progress').css('width',(data.metrics.actorRejected/data.metrics.actorReceived)*100+'%');
      //persistence
      $('#metrics-persistence-received').text(digitUnit(data.metrics.repositoryReceived,0));
      $('#metrics-persistence-accepted').text(digitUnit(data.metrics.repositoryAccepted,0));
      $('#metrics-persistence-rejected').text(digitUnit(data.metrics.repositoryRejected,0));
      $('#metrics-persistence-average-time').text(data.metrics.repositoryAvgTime+' ms / transaction');
      $('#metrics-persistence-received-progress').css('width',data.metrics.repositoryReceived==0?'0%':'100%');
      $('#metrics-persistence-accepted-progress').css('width',(data.metrics.repositoryAccepted/data.metrics.repositoryReceived)*100+'%');
      $('#metrics-persistence-rejected-progress').css('width',(data.metrics.repositoryRejected/data.metrics.repositoryReceived)*100+'%');
      //saga
      $('#metrics-saga-begin').text(digitUnit(data.metrics.sagaBeginCounter,0));
      $('#metrics-saga-end').text(digitUnit(data.metrics.sagaEndCounter,0));
      $('#metrics-saga-average-time').text(data.metrics.sagaAvgTime+' ms / transaction');
      $('#metrics-saga-begin-progress').css('width',data.metrics.sagaBeginCounter==0?'0%':'100%');
      $('#metrics-saga-end-progress').css('width',(data.metrics.sagaEndCounter/data.metrics.sagaBeginCounter)*100+'%');
      //counter
      $('#metrics-committed').text(digitUnit(data.metrics.committed,2));
      $('#metrics-compensated').text(digitUnit(data.metrics.compensated,2));
      $('#metrics-suspended').text(digitUnit(data.metrics.suspended,2));
    },
    error: function (state) {
      // TODO show message
    }
  });

  function digitUnit(n, d) {
    if (n >= 1000) {
      var x = ('' + parseInt(n, 10)).length;
      var d = Math.pow(10, x+1)
      var arr = " kMGTPE";
      x -= x % 3;
      return Math.round(n * d / Math.pow(10, x)) / d + arr[x / 3].trim();
    } else {
      return n;
    }
  }
});