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
    <title>Detail</title>
    <!--<script type="text/javascript" src="assets/raphael.min.js"></script>
    <script type="text/javascript" src="assets/graffle.js"></script>
    <script type="text/javascript" src="assets/graph.js"></script>-->
    <script type="text/javascript" src="assets/jquery.min.js"></script>
    <script type="text/javascript" src="assets/dracula.min.js"></script>
    <!--  The Raphael JavaScript library for vector graphics display  -->
    <script type="text/javascript" src="assets/raphael-min.js"></script>
    <!--  Dracula  -->
    <!--  An extension of Raphael for connecting shapes -->
    <script type="text/javascript" src="assets/dracula_graffle.js"></script>
    <!--  Graphs  -->
    <script type="text/javascript" src="assets/dracula_graph.js"></script>
    <script type="text/javascript" src="assets/dracula_algorithms.js"></script>
    <script type="text/javascript">
      var redraw;
      var height = 300;
      var width = 400;

      var url = "http://127.0.0.1:8080/requests/b";
      var render = function (r, n) {
        /* the Raphael set is obligatory, containing all you want to display */
        var set = r.set().push(
            /* custom objects go here */
            r.rect(n.point[0] - 30, n.point[1] - 13, 60, 44).attr(
                {"fill": "#5b9bd5", r: "12px", "stroke-width": n.distance == 0 ? "3px" : "1px"})).push(
            r.text(n.point[0], n.point[1] + 10, (n.label || n.id)));
        return set;
      };

      var render_false = function (r, n) {
        /* the Raphael set is obligatory, containing all you want to display */
        var set = r.set().push(
            /* custom objects go here */
            r.rect(n.point[0] - 30, n.point[1] - 13, 60, 44).attr(
                {"fill": "#d16d2a", r: "12px", "stroke-width": n.distance == 0 ? "3px" : "1px"})).push(
            r.text(n.point[0], n.point[1] + 10, (n.label || n.id)));
        return set;
      };

      var render_no = function (r, n) {
        /* the Raphael set is obligatory, containing all you want to display */
        var set = r.set().push(
            /* custom objects go here */
            r.rect(n.point[0] - 30, n.point[1] - 13, 60, 44).attr(
                {"fill": "#f2f2f2", r: "12px", "stroke-width": n.distance == 0 ? "3px" : "1px"})).push(
            r.text(n.point[0], n.point[1] + 10, (n.label || n.id)));
        return set;
      };

      var g = new Graph();
      /* modify the edge creation to attach random weights */
      g.edgeFactory.build = function (source, target) {
        var e = jQuery.extend(true, {}, this.template);
        e.source = source;
        e.target = target;
        e.style.label = e.weight = Math.floor(Math.random() * 10) + 1;
        return e;
      }

      /* modify the edge creation to attach random weights */
      g.edgeFactory.build = function (source, target) {
        var e = jQuery.extend(true, {}, this.template);
        e.source = source;
        e.target = target;
        e.style.label = e.weight = Math.floor(Math.random() * 10) + 1;
        return e;
      }

      window.onload = function () {
        $.ajax({
          type: 'GET',
          url: url,
          dataType: "json",
          success: function (datas) {
            console.log(datas);
            //var datas = {"router":{"request-aaa":["request-bbb"],"saga-start":["request-aaa"],"request-bbb":["saga-end"]},"status":{"request-aaa":"OK","request-bbb":"OK"},"error":{}};
            var datas_status = datas.status;
            $.each(datas.router, function (key, value) {
              if (datas_status[key] == 'Failed') {
                g.addNode(key, {render: render_no});
                g.addNode(value, {render: render_no});
                g.addEdge(key, value, {
                  stroke: '#adadad',
                  //fill: '#f2f2f2',
                  label: '',
                  directed: true
                });
              } else if (datas_status[value] == 'Failed') {
                g.addNode(key, {render: render});
                g.addNode(value, {render: render_false});
                g.addEdge(key, value, {
                  stroke: '#adadad',
                  //fill: '#f2f2f2',
                  label: '',
                  directed: true
                });
              } else {
                g.addNode(key, {render: render});
                g.addNode(value, {render: render});
                g.addEdge(key, value, {
                  stroke: '#5b9bd5',
                  //fill: '#5b9bd5',
                  label: '',
                  directed: true
                });
              }
            })

            /* layout the graph using the Spring layout implementation */
            var layouter = new Graph.Layout.Spring(g);
            layouter.layout();

            /* draw the graph using the RaphaelJS draw implementation */
            var renderer = new Graph.Renderer.Raphael('canvas', g, width, height);
            renderer.draw();

            redraw = function () {
              layouter.layout();
              renderer.draw();
            };
          }
        });
      };
    </script>
</head>
<body>
<div id="canvas" style="height: 400px; width: 600px;"></div>
</body>
</html>