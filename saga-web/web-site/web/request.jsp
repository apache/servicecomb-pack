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
   <style type="text/css">
      
        .hide {
            display: none;
        }
       
        .loading,
        .loading::after {
            position: absolute;
            left: 50%;
            top: 50%;
            transform: translate(-50%, -50%);
            -webkit-transform: translate(-50%, -50%);
        }
            .loading,
            .loading span {
                width: 62px;
                height: 62px;
                border-radius: 62px;
            }
        .loading {
            border: 5px solid #d0cecc;
        }
        @-webkit-keyframes loading {
            0 {
                transform: rotate(0);
                -webkit-transform: rotate(0);
            }
            100% {
                transform: rotate(360deg);
                -webkit-transform: rotate(360deg);
            }
        }
        @keyframes loading {
            0 {
                transform: rotate(0);
                -webkit-transform: rotate(0);
            }
            100% {
                transform: rotate(360deg);
                -webkit-transform: rotate(360deg);
            }
        }
        .loading span {
            display: block;
            transform: rotate(0);
            -webkit-transform: rotate(0);
            -webkit-animation: loading 600ms linear infinite;
            animation: loading 600ms linear infinite;
            background: url(/img/nodata.png) no-repeat;
            background-size: 17px 8px;
            background-position: 31px 1px;
        }
        .loading::after {
            content: "loading";
            display: block;
            width: 50px;
            height: 50px;
            line-height: 50px;
            text-align: center;
            font-style: italic;
            color: #fff;
            font-size: 12px;
            border-radius: 50px;
            background: #d0cecc;
        }
        .loading-data img {
            width: 100%;
            max-width: 100%;
            background-size: cover;
        }
       
       
    </style>
</head>
<body>
<ul id="nav">
    <li><a href="request.jsp">Request</a></li>
    <li><a href="result.jsp">Result</a></li>
    <li><a href="#">About</a></li>
</ul>

<h3>Send a new Saga Request</h3>
<textarea style="width:800px;height:600px;"></textarea>
<br>
  <div id="v_loading" class="loading hide"><span></span></div>

       
    <button type="button" id="send-request">Submit</button>
    <button id="send-request">Submit</button>
<div id="msg"></div>
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
                         $('#msg').html('success');
                    }
                });
            })
        })
    </script>
</body>
</html>
