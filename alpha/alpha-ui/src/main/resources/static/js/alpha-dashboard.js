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
      $('#statistics-total').text(digitUnit(data.total,2));
      $('#statistics-successful').text(digitUnit(data.successful,2));
      $('#statistics-compensated').text(digitUnit(data.compensated,2));
      $('#statistics-failed').text(digitUnit(data.failed,2));
      $('#statistics-total-tip').text(data.total);
      $('#statistics-successful-tip').text(data.successful);
      $('#statistics-compensated-tip').text(data.compensated);
      $('#statistics-failed-tip').text(data.failed);
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
    success: function (metrics) {
      refreshActiveTransactionCard(metrics);
    },
    error: function (state) {
      // TODO show message
    }
  });

  var socket = new SockJS('/websocket-config');
  stompClient = Stomp.over(socket);
  stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/metrics', function (metrics) {
      //console.log(JSON.parse(metrics.body).content)
      refreshActiveTransactionCard(JSON.parse(metrics.body))
    });
  });

  function refreshActiveTransactionCard(data){
    //events
    $('#metrics-events-received').text(data.metrics.eventReceived);
    $('#metrics-events-accepted').text(data.metrics.eventAccepted);
    $('#metrics-events-rejected').text(data.metrics.eventRejected);
    $('#metrics-events-average-time').text(data.metrics.eventAvgTime+' ms / event');
    $('#metrics-events-received-progress').css('width',data.metrics.eventReceived==0?'0%':'100%');
    $('#metrics-events-accepted-progress').css('width',(data.metrics.eventAccepted/data.metrics.eventReceived)*100+'%');
    $('#metrics-events-rejected-progress').css('width',(data.metrics.eventRejected/data.metrics.eventReceived)*100+'%');
    //actors
    $('#metrics-actors-received').text(data.metrics.actorReceived);
    $('#metrics-actors-accepted').text(data.metrics.actorAccepted);
    $('#metrics-actors-rejected').text(data.metrics.actorRejected);
    $('#metrics-actors-average-time').text(data.metrics.actorAvgTime+' ms / event');
    $('#metrics-actors-received-progress').css('width',data.metrics.actorReceived==0?'0%':'100%');
    $('#metrics-actors-accepted-progress').css('width',(data.metrics.actorAccepted/data.metrics.actorReceived)*100+'%');
    $('#metrics-actors-rejected-progress').css('width',(data.metrics.actorRejected/data.metrics.actorReceived)*100+'%');
    //persistence
    $('#metrics-persistence-received').text(data.metrics.repositoryReceived);
    $('#metrics-persistence-accepted').text(data.metrics.repositoryAccepted);
    $('#metrics-persistence-rejected').text(data.metrics.repositoryRejected);
    $('#metrics-persistence-average-time').text(data.metrics.repositoryAvgTime+' ms / transaction');
    $('#metrics-persistence-received-progress').css('width',data.metrics.repositoryReceived==0?'0%':'100%');
    $('#metrics-persistence-accepted-progress').css('width',(data.metrics.repositoryAccepted/data.metrics.repositoryReceived)*100+'%');
    $('#metrics-persistence-rejected-progress').css('width',(data.metrics.repositoryRejected/data.metrics.repositoryReceived)*100+'%');
    //saga
    $('#metrics-saga-begin').text(data.metrics.sagaBeginCounter);
    $('#metrics-saga-end').text(data.metrics.sagaEndCounter);
    $('#metrics-saga-average-time').text(data.metrics.sagaAvgTime+' ms / transaction');
    $('#metrics-saga-begin-progress').css('width',data.metrics.sagaBeginCounter==0?'0%':'100%');
    $('#metrics-saga-end-progress').css('width',(data.metrics.sagaEndCounter/data.metrics.sagaBeginCounter)*100+'%');
    //counter
    $('#metrics-committed').text(digitUnit(data.metrics.committed,2));
    $('#metrics-compensated').text(digitUnit(data.metrics.compensated,2));
    $('#metrics-suspended').text(digitUnit(data.metrics.suspended,2));
    $('#metrics-committed-tip').text(data.metrics.committed);
    $('#metrics-compensated-tip').text(data.metrics.compensated);
    $('#metrics-suspended-tip').text(data.metrics.suspended);
  }

  function digitUnit(n, d) {
    x = ('' + n).length, p = Math.pow, d = p(10, d);
    x -= x % 3;
    more = Math.round(n * d / p(10, x)) % d;
    y = Math.round(n * d / p(10, x)) / d + " kMGTPE"[x / 3];
    return more==0?y:y+'+';
  }
});