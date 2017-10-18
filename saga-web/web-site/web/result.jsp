<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Result</title>
    <link rel="stylesheet" href="style.css" type="text/css">
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
            <td><input title="Saga Start Time From" type="datetime-local"/></td>
            <td>To:</td>
            <td><input title="Saga End Time From" type="datetime-local"/></td>
            <td>
                <button>Search</button>
            </td>
        </tr>
    </table>
</div>

<div id="content">
    <table class="table table-bordered table-striped text-center">
        <thead>
        <tr>
            <th>SagaID</th>
            <th>StartTime</th>
            <th>RunPeriod</th>
            <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td>{SagaID}</td>
            <td>{StartTime}</td>
            <td>{RunPeriod}</td>
            <td>{Status}</td>
            <td>
                <button onclick="">Detail</button>
            </td>
        </tr>
        </tbody>
    </table>
</div>

</body>
</html>
