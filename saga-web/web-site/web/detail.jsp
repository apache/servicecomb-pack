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
    <script type="text/javascript" src="assets/raphael.min.js"></script>
    <script type="text/javascript" src="assets/graffle.js"></script>
    <script type="text/javascript" src="assets/graph.js"></script>
    <script type="text/javascript">
      <!--

      var redraw;
      var height = 300;
      var width = 400;

      /* only do all this when document has finished loading (needed for RaphaelJS */
      window.onload = function () {

        var g = new Graph();

        g.addEdge("cherry", "apple");
        g.addEdge("strawberry", "cherry");
        g.addEdge("strawberry", "apple");
        g.addEdge("strawberry", "tomato");
        g.addEdge("tomato", "apple");
        g.addEdge("cherry", "kiwi");
        g.addEdge("tomato", "kiwi");

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
      };

      -->
    </script>
</head>
<body>
<div id="canvas" style="height: 400px; width: 600px;"></div>
</body>
</html>
