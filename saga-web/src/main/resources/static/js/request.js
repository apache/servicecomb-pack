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

window.content = {};
$(function () {
    window.content.requests = [];

    $("#text-switch").bootstrapSwitch();
    $('#parents').multiselect();

    $("#text-switch").on('switchChange.bootstrapSwitch', function (event, state) {
        if (state == true) {
            $('#view-content').hide();
            $('#text-content').show();
        } else {
            $('#text-content').hide();
            $('#view-content').show();
        }
    });

    $('#send-request').click(function () {
        var content = $('#content').val();
        $("#v_loading").show();
        $.ajax({
            type: "POST",
            url: $("#submit-url").val(),
            data: content,
            contentType: "text/plain; charset=utf-8",
            dataType: "json",
            success: function (msg) {
                $("#v_loading").hide();
            },
            error: function (req) {
                alert(req.statusText);
                $("#v_loading").hide();
            }
        });
    });

    $('#policyName').change(function() {
        window.content.policy = $('#policyName option:selected').text();
        $('#content').val(JSON.stringify(window.content, null, 2));
    });

    $("#addRequest").click(function() {
        clearModalValues();
        var requestIds = getRequestIds();
        $("#requestModal").modal('show');
        $("#parents").multiselect("dataprovider", requestIds);

        $("#saveRequest").unbind("click").click(function(){
            window.content.policy = $('#policyName option:selected').text();
            var isUpdatedRequest = saveRequest();
            if (!isUpdatedRequest) {
                var value = $('#type option:selected').text();
                var content = '<tr><td>' + $('#id').val() + '</td><td>' + value + '</td><td>' + $('#service-name').val() +
                    '</td><td><button type="button" class="btn btn-primary requests-view-button" id=' + $('#id').val() +
                    '>View</button></td><td><button class="btn btn-danger requests-remove-button" id=' + $('#id').val() +
                    '>Remove</button></td></tr>';
                $('#requests').append(content);
                $('.requests-view-button').click(function(){
                    var requestId = this.id;
                    var request = {};
                    window.content.requests.some(function(req) {
                        if (req.id == requestId) {
                            request = req;
                            return true;
                        }
                        return false;
                    });
                    loadModalValues(request);
                    $('#requestModal').modal('show');
                });
                $('.requests-remove-button').click(function(){
                    var requestId = this.id;
                    window.content.requests = window.content.requests.filter(function(item) {
                        return item.id != requestId;
                    });
                    $('#content').val(JSON.stringify(window.content, null, 2));
                    $(this).parent().parent().remove();
                });
            }
            $('#requestModal').modal('hide');
            $('#content').val(JSON.stringify(window.content, null, 2));
        });
    });

    addParameter('transaction');
    addParameter('compensation');

    $('#reset').click(function() {
        $('#content').val('');
        window.content = {};
        window.content.requests = [];
        $('#policyName').prop('selectedIndex', 0);
        $('#requests tbody').empty();
    });
});

function clearModalValues() {
    $('#id').val('');
    $('#type').prop('selectedIndex', 0);
    $('#service-name').val('');
    $('#transaction-method').prop('selectedIndex', 0);
    $('#transaction-path').val('');
    $('#transaction-params tr').remove();
    $('#compensation-method').prop('selectedIndex', 0);
    $('#compensation-path').val('');
    $('#compensation-params tr').remove();
    $('#parents').multiselect('deselectAll', true);
    $('#transaction-params-json-div').hide();
    $('#transaction-params-json').val('');
    $('#compensation-params-json-div').hide();
    $('#compensation-params-json').val('');
}

function loadParameterValue(operationName, operation, type) {
    if (type in operation.params && operation.params[type].length != 0) {
        var form = operation.params[type];
        for (var key in form) {
            var content = '<tr><td>' + type + '</td><td><input type="text" value="' + key +
                '"></td><td><input type="text" value="' + form[key] +
                '"></td><td><button class="btn btn-danger remove-button">Remove</button></td></tr>';
            $('#' + operationName + '-params').append(content);
            removeRowFromTable('.remove-button');
        }
    }
}

function loadParameterValues(operationName, operation) {
    loadParameterValue(operationName, operation, 'form');
    loadParameterValue(operationName, operation, 'query');
    if ('json' in operation.params) {
        $('#' + operationName + '-params-json').val(operation.params.json);
        $('#' + operationName + '-params-json-div').show();
    }
}

function loadOperationModalValue(operationName, operation) {
    $('#' + operationName + '-method').val(operation.method);
    $('#' + operationName + '-path').val(operation.path);
    loadParameterValues(operationName, operation);
}

function loadModalValues(request) {
    clearModalValues();
    $('#id').val(request.id);
    $('#type').val(request.type);
    $('#service-name').val(request.serviceName);

    loadOperationModalValue('transaction', request.transaction);
    loadOperationModalValue('compensation', request.compensation);

    var requestIds = getRequestIds();
    requestIds = requestIds.filter(function(item) {
        return item.value != request.id;
    });
    $("#parents").multiselect("dataprovider", requestIds);
    if (request.parents != null && request.parents.length != 0) {
        $('#parents').multiselect('select', request.parents);
    }
}

function retrieveParameterValues(operationName) {
    var parameters = {};
    var query = {};
    var form = {};

    if ($('#' + operationName + '-params').children().length != 0) {
        $('#' + operationName + '-params tr').each(function() {
            var type = $(this).find("td:nth-child(1)").html();
            var key = $(this).find("td:nth-child(2) input").val();
            var value = $(this).find("td:nth-child(3) input").val();
            if (type === 'query') {
                query[key] = value;
            } else {
                form[key] = value;
            }
        });
    }

    if (!isDictEmpty(query)) {
        parameters.query = query;
    }

    if (!isDictEmpty(form)) {
        parameters.form = form;
    }

    var jsonParameter = $('#' + operationName + '-params-json').val();
    if (jsonParameter != '') {
        parameters.json = jsonParameter;
    }
    return parameters;
}

function retrieveOperationValues(operationName) {
    var operation = {};
    operation.method = $('#' + operationName + '-method option:selected').text();
    operation.path = $('#' + operationName + '-path').val();
    operation.params = retrieveParameterValues(operationName);
    return operation;
}

function saveRequest() {
    var request = {};
    request.id = $('#id').val();
    request.type = $('#type option:selected').text();
    request.serviceName = $('#service-name').val();

    var transaction = retrieveOperationValues('transaction');
    if (!isDictEmpty(transaction)) {
        request.transaction = transaction;
    }

    var compensation = retrieveOperationValues('compensation');
    if (!isDictEmpty(compensation)) {
        request.compensation = compensation;
    }

    var parents = [];
    $("#parents option:selected").each(function() {
        parents.push(this.text);
    });
    if (parents.length != 0) {
        request.parents = parents;
    }

    var isRequestUpdated = false;
    window.content.requests.some(function(obj, index, arr) {
        if (obj.id == request.id) {
            arr[index] = request;
            isRequestUpdated = true;
            return true;
        }
        return false;
    });
    if (!isRequestUpdated) {
        window.content.requests.push(request);
    }
    return isRequestUpdated;
}

function getRequestIds() {
    var requests = window.content.requests;
    var requestIds = [];
    requests.forEach(function(req) {
        requestIds.push({"label": req.id, "value": req.id});
    });
    return requestIds;
}

function addParameterRowInTable(clickableSelector, tableSelector, type) {
    $(clickableSelector).click(function() {
        $(tableSelector).append('<tr><td>' + type + '</td><td><input></td><td><input></td><td><button class="btn btn-danger remove-button">Remove</button></td></tr>');
        removeRowFromTable('.remove-button');
    });
}

function addJsonParameter(clickableSelector, textAreaSelector) {
    $(clickableSelector).click(function() {
        $(textAreaSelector).show();
    });
}

function addParameter(operationName) {
    addParameterRowInTable('#' + operationName + '-new-query-btn', '#' + operationName + '-params', 'query');
    addParameterRowInTable('#' + operationName + '-new-form-btn', '#' + operationName + '-params', 'form');
    addJsonParameter('#' + operationName + '-new-json-btn', '#' + operationName + '-params-json-div');
}

function removeRowFromTable(buttonSelector) {
    $(buttonSelector).click(function() {
        $(this).parent().parent().remove();
    });
}

function isDictEmpty(obj) {
    return Object.keys(obj).length === 0;
}
