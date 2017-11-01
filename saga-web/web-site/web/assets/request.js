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
            data: "content=" + content,
            dataType: "text",
            success: function (msg) {
                $("#v_loading").hide();
                alert(msg);
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

    $('#transaction-params-btn').click(function() {
        $('#transaction-params').append('<tr><td><input></td><td><input></td><td><button class="btn btn-danger remove-button">Remove</button></td></tr>');
        $('.remove-button').click(function() {
            $(this).parent().parent().remove();
        });
    });

    $('#compensation-params-btn').click(function() {
        $('#compensation-params').append('<tr><td><input></td><td><input></td><td><button class="btn btn-danger remove-button">Remove</button></td></tr>');
        $('.remove-button').click(function() {
            $(this).parent().parent().remove();
        });
    });

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
}

function loadModalValues(request) {
    // TODO
    clearModalValues();
    $('#id').val(request.id);
    $('#type').val(request.type);
    $('#service-name').val(request.serviceName);
    $('#transaction-method').val(request.transaction.method);
    $('#transaction-path').val(request.transaction.path);
    if (request.transaction.params.form.length != 0) {
        var transactionForms = request.transaction.params.form;
        for (var key in transactionForms) {
            var transactionContent = '<tr><td><input type="text" value="' + key +
                '"></td><td><input type="text" value="' + transactionForms[key] +
            '"></td><td><button class="btn btn-danger remove-button">Remove</button></td></tr>';
            $('#transaction-params').append(transactionContent);
            $('.remove-button').click(function() {
                $(this).parent().parent().remove();
            });
        }
    }
    $('#compensation-method').val(request.compensation.method);
    $('#compensation-path').val(request.compensation.path);
    if (request.compensation.params.form.length != 0) {
        var compensationForms = request.compensation.params.form;
        for (var compensationKey in compensationForms) {
            var compensationContent = '<tr><td><input type="text" value="' + compensationKey +
                '"></td><td><input type="text" value="' + compensationForms[compensationKey] +
                '"></td><td><button class="btn btn-danger remove-button">Remove</button></td></tr>';
            $('#compensation-params').append(compensationContent);
            $('.remove-button').click(function() {
                $(this).parent().parent().remove();
            });
        }
    }
    var requestIds = getRequestIds();
    requestIds = requestIds.filter(function(item) {
        return item.value != request.id;
    });
    $("#parents").multiselect("dataprovider", requestIds);
    if (request.parents != null && request.parents.length != 0) {
        $('#parents').multiselect('select', request.parents);
    }
}

function saveRequest() {
    var request = {};
    request.id = $('#id').val();
    request.type = $('#type option:selected').text();
    request.serviceName = $('#service-name').val();
    var transaction = {};
    transaction.method = $('#transaction-method option:selected').text();
    transaction.path = $('#transaction-path').val();
    transaction.params = {'form': {}};
    if ($('#transaction-params').children().length != 0) {
        $('#transaction-params tr').each(function() {
            var key = $(this).find("td:first input").val();
            var value = $(this).find("td:nth-child(2) input").val();
            transaction.params.form[key] = value;
        });
    }
    request.transaction = transaction;

    var compensation = {};
    compensation.method = $('#compensation-method option:selected').text();
    compensation.path = $('#compensation-path').val();
    compensation.params = {'form': {}};
    if ($('#compensation-params').children().length != 0) {
        $('#compensation-params tr').each(function() {
            var key = $(this).find("td:first input").val();
            var value = $(this).find("td:nth-child(2) input").val();
            compensation.params.form[key] = value;
        });
    }
    request.compensation = compensation;

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
