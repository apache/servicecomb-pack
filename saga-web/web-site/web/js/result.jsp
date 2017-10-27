<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Result</title>
    <link rel="stylesheet" href="style.css" type="text/css">
    <link rel="stylesheet" href="/assets/bootstrap-table.css">

    <script src="/assets/jquery.min.js"></script>
    <script src="/assets/bootstrap/js/bootstrap.min.js"></script>
    <script src="/js/examples.js"></script>
    <script src="/assets/bootstrap-table.js"></script>
	<script src="/js/date.js"></script>
	
</head>
<body>
<ul id="nav">
    <li><a href="result.jsp">Result</a></li>
    <li><a href="#">About</a></li>
</ul>

<div id="search">
    <table>
        <tr>
            <td>From:</td>
            <td><input  onclick="SetDate(this,'yyyy-MM-dd hh:mm:ss')" /></td>
            <td>To:</td>
            <td><input  onclick="SetDate(this,'yyyy-MM-dd hh:mm:ss')"/></td>
            <td>
                <button>Search</button>
            </td>
        </tr>
    </table>
</div>
<div id="content"></div>

<script src="table.js"></script>
</body>
</html>
