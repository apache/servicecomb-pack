<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Result</title>
    <link rel="stylesheet" href="style.css" type="text/css">
	
	<link id="uiThemes" rel="stylesheet" type="text/css" media="screen" href="styles/themes/redmond/jquery-ui-1.7.2.custom.css" />
    <link rel="stylesheet" type="text/css" media="screen" href="styles/themes/ui.jqgrid.css" />
    <!-- 引入jQuery -->
    <script type="text/javascript" src="scripts/jQuery/jquery-1.3.2.js"></script>
    <script src="scripts/jQuery/plugins/jquery-ui-1.7.2.custom.min.js" type="text/javascript"></script>
    <script src="scripts/jQuery/plugins/grid.locale-zh_CN.js" type="text/javascript"></script>
    <script src="scripts/jQuery/plugins/jquery.jqGrid.min.js" type="text/javascript"></script>
	
	
	<script src="result.js" type="text/javascript"></script>
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
	<!-- add datagrid start -->
	<table id="list47"></table>
	<div id="plist47"></div>
	<!-- add datagrid end -->
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
