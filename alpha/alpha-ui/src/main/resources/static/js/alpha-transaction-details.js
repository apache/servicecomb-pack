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
  $('i[name="event_more"]').click(function (event) {
    var more_id = '#'+$(event.target).attr("target")
    if($(event.target).hasClass('fa-caret-square-down')){
      $(event.target).removeClass("fa-caret-square-down")
      $(event.target).addClass("fa-caret-square-up")
    }else{
      $(event.target).removeClass("fa-caret-square-up")
      $(event.target).addClass("fa-caret-square-down")
    }
    $(more_id).toggleClass("d-none")
  })
});