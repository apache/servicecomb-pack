<%--
  ~   Copyright 2017 Huawei Technologies Co., Ltd
  ~
  ~   Licensed under the Apache License, Version 2.0 (the "License");
  ~   you may not use this file except in compliance with the License.
  ~   You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~   Unless required by applicable law or agreed to in writing, software
  ~   distributed under the License is distributed on an "AS IS" BASIS,
  ~   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~   See the License for the specific language governing permissions and
  ~   limitations under the License.
  --%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Request</title>
    <link rel="stylesheet" href="style.css" type="text/css">
    <link rel="stylesheet" href="request.css" type="text/css">
    <script src="assets/jquery.min.js"></script>
</head>
<body>
<ul id="nav">
    <li><a href="request.jsp">Request</a></li>
    <li><a href="result.jsp">Result</a></li>
    <li><a href="#">About</a></li>
</ul>

<h3>Send a new Saga Request</h3>
<textarea id="content" style="width:800px;height:600px;"></textarea>
<br>
<div id="v_loading" class="loading hide"><span></span></div>
<button type="button" id="send-request">Submit</button>
<script type="text/javascript">
  $(function () {
    $('#send-request').click(function () {
      var content = $('#content').val();
      $("#v_loading").show();
      $.ajax({
        type: "POST",
        url: "http://localhost:8080/requests",
        data: "content=" + content,
        dataType: "text",
        success: function (msg) {
          $("#v_loading").hide();
          alert(msg);
        }
      });
    })
  })
</script>
</body>
</html>