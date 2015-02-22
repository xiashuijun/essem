function handleXHRError(xhr, textStatus, errorThrown) {
    alert(xhr.code);
    alert(xhr.responseText);
}

function getId(xid) {
    var index = xid.indexOf('_');
    if (index > 0 && index < xid.length - 1) {
        return xid.substring(index + 1);
    } else {
        return "";
    }
}

function scrollToTop() {
    $("html, body").animate({ scrollTop: 0}, 0);
}

function loadSubContent(html) {
    var content = $('#subcontent');
    content.fadeOut('fast',function(){
        content.html(html);
        content.fadeIn('fast', scrollToTop());
    });
}

function activateApp(app) {
    $('.app-nav').removeClass('active');
    $('.app-select').removeClass('m_highlight');
    $('#app_'+app).addClass('active');
    $('#mapp_'+app).addClass('m_highlight');
}

function activateRange(range) {
    $('.range-select').removeClass('m_highlight');
    $('.range-nav').removeClass('active');
    $('#rn_'+range).addClass('active');
    $('#rs_'+range).addClass('m_highlight');
}

function activateHost(host) {
    $('.host-select').removeClass('m_highlight');
    $('.host-nav').removeClass('active');
    $('#hn_'+host).addClass('active');
    $('#hs_'+host).addClass('m_highlight');
}

function setCurrentTimezone(display) {
    jQuery('#set_tz').text(display);
}

function bindApps(index) {
    $('.app-nav').click(function(event) {
        event.preventDefault();
        var app = $(this).attr('name');
        bindMetricsFilter(index, app);
        activateApp(app);
        loadMetrics(index, app);
    });
}

function bindTopBar(index) {
    $('#topbar_status').click(function(event) {
        event.preventDefault();
        var url = '/console/' + index + '/stats';
        $.ajax({
            type: 'GET',
            url: url,
            dataType: 'html',
            success: function(html, textStatus) {
                $('#top_modal_content').html(html);
                $('#top_modal').foundation('reveal', 'open');
            },
            error: handleXHRError
        });
    });

    $('#topbar_dash').click(function(event) {
        event.preventDefault();
        var url = '/console/' + index + '/usergraphs';
        $.ajax({
            type: 'GET',
            url: url,
            dataType: 'html',
            success: function(html, textStatus) {
                $('#top_modal_content').html(html);
                $('#top_modal').foundation('reveal', 'open');
            },
            error: handleXHRError
        });
    });
}

function bindMetricsFilter(index, app) {
    $('input:radio').change(function(event) {
        loadMetrics(index, app);
    });
}

function currTypeFilter() {
    return $('input[name=tf]:checked').val();
}

function currActivityFilter() {
    return $('input[name=af]:checked').val();
}

function currPrefix() {
    return $('#mf').val();
}

function bindFieldChecks() {
    $('input:checkbox').change(function(event) {
        var box = $(this);
        var gid = '#gr_'+getId(this.id);

        if(box.prop('checked')) {
            $(gid).show('fast');
        } else {
            $(gid).hide('fast');
        }
    });
}

function hideCheckedFields() {
    $('input:checkbox').each(function(index) {
        var box = $(this);
        var gid = '#gr_'+getId(this.id);
        if(box.prop('checked')) {
            $(gid).hide('fast');
        }
    });
}

function showCheckedFields() {
    $('input:checkbox').each(function(index) {
        var box = $(this);
        var gid = '#gr_'+getId(this.id);
        if(box.prop('checked')) {
            $(gid).show('fast');
        }
    });
}

function buildMetricsEndpoint(index, app, overridePrefix) {

    var type = currTypeFilter();
    var filter = currActivityFilter();
    var prefix = overridePrefix;
    if(prefix == null) {
        prefix = currPrefix();
    }

    var url = '/console/' + index + '/metrics/' + app + '/' + type + "?filter=" + filter;

    if(prefix != null && prefix.length > 0) {
        return url + '&prefix=' + encodeURIComponent(prefix);
    } else {
        return url;
    }
}

function loadMetrics(index, app) {
    var url = buildMetricsEndpoint(index, app, null);
    $.ajax({
        type: 'GET',
        url: url,
        dataType: 'html',
        success: function(html, textStatus) {
            loadSubContent(html);
            bindMetricSearchBox(index, app);
        },
        error: handleXHRError
    })
}

var MINUTE_MILLIS = 60000;
var HOUR_MILLIS = MINUTE_MILLIS * 60;
var DAY_MILLIS = HOUR_MILLIS * 24;
var WEEK_MILLIS = DAY_MILLIS * 7;
var MONTH_MILLIS = DAY_MILLIS * 30;
var YEAR_MILLIS = DAY_MILLIS * 365;

function rangeMillis(range) {
    switch(range.toLowerCase()) {
        case 'minute': return MINUTE_MILLIS;
        case 'hour': return HOUR_MILLIS;
        case 'day': return DAY_MILLIS;
        case 'week': return WEEK_MILLIS;
        case 'month': return MONTH_MILLIS;
        case 'year': return YEAR_MILLIS;
        default: return HOUR_MILLIS;
    }
}

function downsampleInterval(range) {
    switch(range.toLowerCase()) {
        case 'minute': return 'second';
        case 'hour': return '5s';
        case 'day': return '5m';
        case 'week': return 'hour';
        case 'month': return 'hour';
        case 'year': return 'day';
        default: return 'hour';
    }
}

function xFormatForRange(range) {
    switch(range.toLowerCase()) {
        case 'minute': return '%X';
        case 'hour': return '%X';
        case 'day': return '%X';
        case 'week': return '%x';
        case 'month': return '%x';
        case 'year': return '%x';
        default: return '%x';
    }
}

var KEYUP_REFRESH_MILLIS = 400;

/*
  Converts the 'date' field to a JS date using the 'timestamp' field.
 */
function convertTimestamps(data) {
    data = data.map(function(d) {
        d.date = new Date(d.timestamp);
        return d;
    });
    return data;
}

/*
  Sets the 'date' field to a JS date...but first...
  offset the UTC timestamp so that D3/mgraphics display dates/times
  in the target timezone.
 */
function changeTimezone(data, tz) {
    //Technically this is incorrect as DST changes will not be reflected
    //But...really too slow to do on every point.
    var localOffsetMinutes = new Date().getTimezoneOffset();
    var targetOffsetMinutes = moment.tz.zone(tz).offset(new Date().getTime());
    var offsetMillis = (localOffsetMinutes - targetOffsetMinutes) * 60 * 1000;
    data = data.map(function(d) {
        d.date = new Date(d.timestamp + offsetMillis);
        return d;
    });
    return data;
}

/*
  Determine if the specified timezone string has an offset
  that matches the local timezone as reported by JS.
 */
function offsetMatchesLocal(tz) {
    var currTime = new Date();
    var localOffsetMinutes = currTime.getTimezoneOffset();
    var checkOffsetMinutes = moment.tz.zone(tz).offset(currTime.getTime());
    return localOffsetMinutes == checkOffsetMinutes;
}

function bindMetricSearchBox(index, app) {

    var mf = $('#mf');

    $('#mf-clear').click(function(event) {
        mf.val('');
        loadMetrics(index, app);
    });

    var athread = null;
    function displayMatches(prefix) {
        $.ajax({
            type: 'GET',
            url: buildMetricsEndpoint(index, app, prefix),
            dataType: 'html',
            success: function(html, textStatus) {
                loadSubContent(html);
            },
            error: handleXHRError
        })
    }
    mf.unbind();
    mf.keyup(function() {
        clearTimeout(athread);
        var $this = $(this); athread = setTimeout(function(){displayMatches($this.val())}, KEYUP_REFRESH_MILLIS);
    });
}

function loadMetricData(config, renderFn) {

    setDynamicTitles(config);
    $('#gloading').show();

    var rangeComponent = '&range=' + config.range;
    if(config.startTimestamp > 0 && config.endTimestamp > 0) {
        rangeComponent = rangeComponent + '&rangeStart=' + config.startTimestamp +'&rangeEnd=' + config.endTimestamp;
    }

    var graphURL =
        "/mgraph/"+config.index+"/graph?emptyBins=false&aggregateOn=name&downsampleTo="+config.downsampleInterval+
        "&downsampleFn="+config.downsampleFn+rangeComponent+"&limit=5000&name="+encodeURIComponent(config.name)+
        "&app="+config.app+"&host="+config.host;

    $('#json-link').attr('href', graphURL);

    $.getJSON(graphURL, function(data) {
        if(config.tz == null || config.tz == '') {
            data = convertTimestamps(data);
        } else {
            data = changeTimezone(data, config.tz);
        }
        if(renderFn != null) renderFn(data);
    });
}

function rangeDetail(config) {

    var mformat0 = 'YYYY-MM-DD HH:mm:ss';
    var mformat1 = 'YYYY-MM-DD HH:mm:ss Z z';

    var rangeDetail = '';
    if(config.tz != null && config.tz != '') {
        rangeDetail = moment.tz(config.startTimestamp, config.tz).format(mformat0) + ' - ' +
        moment.tz(config.endTimestamp, config.tz).format(mformat1);
    } else {
        rangeDetail = moment(config.startTimestamp).format(mformat0) + ' - ' + moment(config.endTimestamp).format(mformat0);
    }
    return rangeDetail;
}

function setDynamicTitles(config) {

    if(config.host != '') {
        $('#app-title').html(config.app + '&nbsp;&raquo;&nbsp;' + config.host);
    } else {
        $('#app-title').text(config.app);
    }

    $('#range-detail').text(rangeDetail(config));

    var permalinkURL = "/console/"+config.index+"/graphs/"+config.app+
        "?name="+encodeURIComponent(config.name)+
        "&host="+config.host+
        "&range="+config.range+
        "&downsampleTo="+downsampleInterval(config.range)+
        "&downsampleFn="+config.downsampleFn +
        "&rangeStart="+config.startTimestamp+"&rangeEnd="+config.endTimestamp;

    $('#range-permalink').attr('href', permalinkURL);
}

function samplePlural(samples) {
    if(samples == 1) {
        return samples + ' Sample';
    } else if(samples == 0) {
        return 'No Samples';
    } else {
        return samples + ' Samples';
    }
}

function loadGraph(data, config, dataConfig) {

    var target = dataConfig.target;
    if(target == '') {
        target = '#m_' + config.name_hash + '_' + config.field;
    }

    var xAxisFormatter = d3.time.format(xFormatForRange(dataConfig.range));

    var hoverFormatter = d3.time.format('%Y-%m-%d %H:%M:%S');
    if(dataConfig.tz != null && dataConfig.tz != '') {
        var tzf = moment.tz(dataConfig.tz).format("Z z");
        hoverFormatter = d3.time.format('%Y-%m-%d %H:%M:%S ' + tzf);
    }

    var left_margin = config.left_margin != null ? config.left_margin : 40;
    var bottom_margin = config.bottom_margin != null ? config.bottom_margin : 40;

    MG.data_graphic({
        area: true,
        missing_is_zero: false,
        interpolate: 'linear', //linear
        animate_on_load: true,
        data: data,
        width: dataConfig.width,
        height: dataConfig.height,
        left: left_margin,
        bottom: bottom_margin,
        buffer: 0,
        show_years: false,
        xax_tick: 0,
        xax_count: 6,
        small_text: false,
        y_extended_ticks: true,
        target: target,
        x_accessor: 'date',
        xax_format: xAxisFormatter,
        y_accessor: config.field,
        min_y_from_data: false,
        inflator: 1.2, //Default: 10/9
        mouseover: function(d, i) {
            var content = d[config.field].toFixed(3)+' '+config.y_label+' '+hoverFormatter(d.date)+' '+' ('+ samplePlural(d.samples)+')';
            $(target+' svg .mg-active-datapoint').text(content);
        }
    });
}

function showFieldStats(config, field, range) {
    var url = '/console/' + config.index + '/fstats/' + config.app + '?name=' + config.name + '&field=' + field + '&range=' + range;
    if(config.host != null) url = url + '&host=' + config.host;
    $.ajax({
        type: 'GET',
        url: url,
        dataType: 'html',
        success: function(html, textStatus) {
            $('#top_modal_content').html(html);
            $('#fstats-range-detail').text(rangeDetail(config));
            var fieldTitle = $('#fstats_' + field).attr('title');
            $('#fstats-field-title').text(fieldTitle);
            $('#top_modal').foundation('reveal', 'open');
            scrollToTop();
        },
        error: handleXHRError
    });
}

function renderFieldStats(config, field, target) {
    var url = '/console/' + config.index + '/fstats/' + config.app + '?name=' + config.name + '&field=' + field + '&range=' + config.range;
    if(config.host != null) url = url + '&host=' + config.host;
    if(config.startTimestamp > 0) url = url + '&startTimestamp=' + config.startTimestamp;
    if(config.endTimestamp > 0) url = url + '&endTimestamp=' + config.endTimestamp;
    if(config.t != null) url = url + '&t=' + config.t;

    $.ajax({
        type: 'GET',
        url: url,
        dataType: 'html',
        success: function(html, textStatus) {
            $(target).html(html);
        },
        error: handleXHRError
    });
}

function showGraphSave(config, field) {

    var rangeComponent = '&range=' + config.range;
    if(config.startTimestamp > 0 && config.endTimestamp > 0) {
        rangeComponent = rangeComponent + '&rangeStart=' + config.startTimestamp +'&rangeEnd=' + config.endTimestamp;
    }

    var url = '/console/' + config.index + '/savegraph/' + config.app + '?name=' + encodeURIComponent(config.name) + '&field=' + field +
            '&host=' + config.host + '&downsampleFn=' + config.downsampleFn + rangeComponent;

    if(config.host != null) url = url + '&host=' + config.host;
    $.ajax({
        type: 'GET',
        url: url,
        dataType: 'html',
        success: function(html, textStatus) {
            $('#top_modal_content').html(html);
            $('#top_modal').foundation('reveal', 'open');
            scrollToTop();
            $('#save-key-form').submit(function(event) {
                event.preventDefault();
                saveGraph(config.index, config.app, jQuery(this).serialize());
            });
        },
        error: handleXHRError
    });
}

function saveGraph(index, app, data) {
    $.ajax({
        type: 'POST',
        url: '/console/' + index + '/savegraph/' + app,
        data: data,
        success: function (html, textStatus) {
            if (textStatus == "success") {
                window.location = '/console/' + index + '/usergraph/' + html.trim();
            } else {
                alert("Error: " + textStatus);
            }
        },
        error: handleXHRError
    });
}

function deleteGraph(index, id, hideId) {
    $.ajax({
        type: 'DELETE',
        url: '/console/' + index + '/deletegraph/' + id,
        success: function (html, textStatus) {
            if (textStatus == "success") {
                $('#' + hideId).fadeOut('slow');
            } else {
                alert("Error: " + textStatus);
            }
        },
        error: handleXHRError
    });
}