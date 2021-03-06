/* globals moment, d3, MG */

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
    $('#set_tz').text(display);
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
    if(prefix) {
        prefix = currPrefix();
    }

    var url = '/console/' + index + '/metrics/' + app + '/' + type + "?filter=" + filter;

    if(prefix) {
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
    });
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
        case 'minute': return '%H:%M';
        case 'hour': return '%H:%M';
        case 'day': return '%H:%M';
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
    return localOffsetMinutes === checkOffsetMinutes;
}

function bindMetricSearchBox(index, app) {

    var mf = $('#mf');

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
        });
    }
    mf.unbind();
    mf.keyup(function() {
        clearTimeout(athread);
        var $this = $(this); athread = setTimeout(function() {
            displayMatches($this.val());
        }, KEYUP_REFRESH_MILLIS);
    });
    mf.keydown(function(event) {
        if (event.which === 27 ) {
            event.preventDefault();
            mf.val('');
        }
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
        "&app="+config.app+"&host="+config.host+"&rateUnit="+config.rateUnit;

    $('#json-link').attr('href', graphURL);

    $.getJSON(graphURL, function(data) {
        if(!config.tz) {
            data = convertTimestamps(data);
        } else {
            data = changeTimezone(data, config.tz);
        }
        if(renderFn) {
            renderFn(data);
        }
    });
}

function loadHistogramData(config, renderFn) {

    //setDynamicTitles(config);
    $('#gloading').show();

    var rangeComponent = '?range=' + config.range;
    if(config.startTimestamp > 0 && config.endTimestamp > 0) {
        rangeComponent = rangeComponent + '&rangeStart=' + config.startTimestamp +'&rangeEnd=' + config.endTimestamp;
    }

    var graphURL =
        '/mgraph/'+config.index+'/histogram' + rangeComponent +
        '&name='+encodeURIComponent(config.name) +
        '&app='+config.app+'&host='+config.host +
        '&units=millis';

    $('#json-link').attr('href', graphURL);

    $.getJSON(graphURL, function(data) {
        if(renderFn) {
            renderFn(data.bin);
        }
    });
}

function rangeDetail(config) {

    var mformat0 = 'YYYY-MM-DD HH:mm:ss';
    var mformat1 = 'YYYY-MM-DD HH:mm:ss Z z';

    var rangeDetailStr = '';
    if(config.tz) {
        rangeDetailStr = moment.tz(config.startTimestamp, config.tz).format(mformat0) + ' - ' +
        moment.tz(config.endTimestamp, config.tz).format(mformat1);
    } else {
        rangeDetailStr = moment(config.startTimestamp).format(mformat0) + ' - ' + moment(config.endTimestamp).format(mformat0);
    }
    return rangeDetailStr;
}

function setDynamicTitles(config) {

    if(config.host) {
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
    setDynamicLabels(config);
}

function setDynamicLabels(config) {

    var rateUnit = "perSecond";
    if(config.rateUnit) {
        rateUnit = config.rateUnit;
    }

    switch(rateUnit.toLowerCase()) {
        case "perminute":
            $('#yl_m1Rate').text("Per Minute");
            $('#yl_m5Rate').text("Per Minute");
            $('#yl_m15Rate').text("Per Minute");
            $('#yl_meanRate').text("Per Minute");
            break;
        case "perhour":
            $('#yl_m1Rate').text("Per Hour");
            $('#yl_m5Rate').text("Per Hour");
            $('#yl_m15Rate').text("Per Hour");
            $('#yl_meanRate').text("Per Hour");
            break;
        default:
            $('#yl_m1Rate').text("Per Second");
            $('#yl_m5Rate').text("Per Second");
            $('#yl_m15Rate').text("Per Second");
            $('#yl_meanRate').text("Per Second");
    }
}

function samplePlural(samples) {
    if(samples === 1) {
        return samples + ' Sample';
    } else if(samples === 0) {
        return 'No Samples';
    } else {
        return samples + ' Samples';
    }
}

function loadGraph(data, config, dataConfig) {

    var target = dataConfig.target;
    if(!target) {
        target = '#m_' + config.name_hash + '_' + config.field;
    }

    var xFormat = xFormatForRange(dataConfig.range);
    var xAxisFormatter = d3.time.format(xFormat);

    var hoverFormatter = d3.time.format('%Y-%m-%d %H:%M:%S');
    if(dataConfig.tz) {
        var tzf = moment.tz(dataConfig.tz).format("Z z");
        hoverFormatter = d3.time.format('%Y-%m-%d %H:%M:%S ' + tzf);
    }

    var left_margin = config.left_margin ? config.left_margin : 60;
    var bottom_margin = config.bottom_margin ? config.bottom_margin : 40;
    var full_width = config.full_width ? config.full_width : false;
    var full_height = config.full_height ? config.full_height : false;

    var x_label = "";
    var y_label = "";

    if(config.with_labels) {
        x_label = config.x_label ? config.x_label : "";
        y_label = config.y_label ? config.y_label : "";
    }
    var title = "";
    if(config.with_title) {
        title = config.title ? config.title : "";
    }

    var small_text = config.small_text ? config.small_text : false;

    var hover_label = config.y_label;

    if(config.field.endsWith("Rate") && dataConfig.rateUnit) {
        switch(dataConfig.rateUnit.toLowerCase()) {
            case "perminute":
                hover_label = "Per Minute";
                break;
            case "perhour":
                hover_label = "Per Hour";
                break;
            case "persecond":
                hover_label = "Per Second";
                break;
        }
    }

    MG.data_graphic({
        area: true,
        missing_is_zero: false,
        missing_is_hidden: true,
        interpolate: 'linear', //linear
        animate_on_load: true,
        data: data,
        width: dataConfig.width,
        full_width: full_width,
        full_height: full_height,
        height: dataConfig.height,
        left: left_margin,
        bottom: bottom_margin,
        buffer: 0,
        show_secondary_x_label: false,
        xax_tick: 0,
        xax_count: 6,
        small_text: small_text,
        y_extended_ticks: true,
        target: target,
        x_accessor: 'date',
        xax_format: xAxisFormatter,
        y_accessor: config.field,
        min_y_from_data: false,
        title: title,
        x_label: x_label,
        y_label: y_label,
        inflator: 1.2, //Default: 10/9
        mouseover: function(d, i) {
            var content = d[config.field].toFixed(3)+' '+hover_label+' '+hoverFormatter(d.date)+' '+' ('+ samplePlural(d.samples)+')';
            $(target+' svg .mg-active-datapoint').text(content);
        }
    });
}

function showFieldStats(config, field, range) {
    var url = '/console/' + config.index + '/fstats/' + config.app + '?name=' + config.name + '&field=' + field + '&range=' + range;
    if(config.host) {
        url = url + '&host=' + config.host;
    }
    if(config.rateUnit) {
        url = url + '&rateUnit=' + config.rateUnit;
    }

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
    if(config.host) {
        url = url + '&host=' + config.host;
    }
    if(config.startTimestamp > 0) {
        url = url + '&startTimestamp=' + config.startTimestamp;
    }
    if(config.endTimestamp > 0) {
        url = url + '&endTimestamp=' + config.endTimestamp;
    }
    if(config.t) {
        url = url + '&t=' + config.t;
    }
    if(config.rateUnit) {
        url = url + '&rateUnit=' + config.rateUnit;
    }

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

    if(config.host) {
        url = url + '&host=' + config.host;
    }
    if(config.rateUnit) {
        url = url + '&rateUnit=' + config.rateUnit;
    }

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
                saveGraph(config.index, config.app, $(this).serialize());
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
            if (textStatus === "success") {
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
            if (textStatus === "success") {
                $('#' + hideId).fadeOut('slow');
            } else {
                alert("Error: " + textStatus);
            }
        },
        error: handleXHRError
    });
}

function loadHistogram(data, config, dataConfig) {

    var target = dataConfig.target;
    if(!target) {
        target = '#m_' + config.name_hash + '_' + config.field;
    }

    var left_margin = config.left_margin ? config.left_margin : 60;
    var bottom_margin = config.bottom_margin ? config.bottom_margin : 40;
    var full_width = config.full_width ? config.full_width : false;
    var full_height = config.full_height ? config.full_height : false;

    var x_label = "";
    var y_label = "";

    if(config.with_labels) {
        x_label = config.x_label ? config.x_label : "";
        y_label = config.y_label ? config.y_label : "";
    }
    var title = "";
    if(config.with_title) {
        title = config.title ? config.title : "";
    }

    var small_text = config.small_text ? config.small_text : false;

    MG.data_graphic({
        chart_type: 'histogram',
        binned: true,
        bar_margin: 1,
        animate_on_load: true,
        data: data,
        width: dataConfig.width,
        full_width: full_width,
        full_height: full_height,
        height: dataConfig.height,
        left: left_margin,
        bottom: bottom_margin,
        buffer: 0,
        show_secondary_x_label: false,
        small_text: small_text,
        y_extended_ticks: true,
        target: target,
        x_accessor: 'percentile',
        xax_tick: 1,
        xax_count: 20,
        //xax_format: xAxisFormatter,
        //y_accessor: config.field,
        y_accessor: 'count',
        min_y_from_data: false,
        title: title,
        x_label: x_label,
        y_label: y_label,
        mouseover: function(d, i) {
            d3.select(target + ' svg .mg-active-datapoint')
                .text('Value: ' + d3.round(d.x,2) +  '   Count: ' + d.y);
        }
    });
}