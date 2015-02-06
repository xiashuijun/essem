graphs_main(index, indexList, app, host, downsampleList, zoneList, metric, graphs, range) ::= <<
<!doctype html>
<html class="no-js" lang="en">
$head()$
<body>
$topbar(index, indexList, zoneList)$
<div class="row">
    <div id="sidebar" class="large-3 columns">
        <h5 class="subheader">Range</h5>
        <ul class="side-nav">
            <li id="rs_hour" class="range-select"><a id="rn_hour" class="range-nav" name="hour" href="#">Hour</a></li>
            <li id="rs_day" class="range-select"><a id="rn_day" class="range-nav" name="day" href="#">Day</a></li>
            <li id="rs_week" class="range-select"><a id="rn_week" class="range-nav" name="week" href="#">Week</a></li>
            <li id="rs_month" class="range-select"><a id="rn_month" class="range-nav" name="month" href="#">Month</a>
            </li>
        </ul>

        $if(metric.multiField)$
        <h5 class="subheader">Graphs</h5>

        <div class="gfselect">
            $graphs:{graph |
            $if(graph.active)$
            <span class="nowrap"><input name="msfield" id="msfield_$graph.field$" type="checkbox"
                                        checked="checked"><label for="msfield_$graph.field$">$graph.shortTitle$</label></span>
            $else$
            <span class="nowrap"><input name="msfield" id="msfield_$graph.field$" type="checkbox"><label
                    for="msfield_$graph.field$">$graph.shortTitle$</label></span>
            $endif$
            }$
        </div>
        $endif$

        <h5 class="subheader">Downsample With</h5>

        <div class="gfselect">
            $downsampleList:{downsampleFn |
            <span class="nowrap">
                $if(downsampleFn.active)$
                <input class="dfs" type="radio" checked="checked" name="dsfield" value="$downsampleFn.functionName$"
                       id="dsfield_$downsampleFn.functionName$">
                $else$
                <input class="dfs" type="radio" name="dsfield" value="$downsampleFn.functionName$"
                       id="dsfield_$downsampleFn.functionName$">
                $endif$
                <label for="dsfield_$downsampleFn.functionName$">$downsampleFn.displayName$</label>
            </span>
            }$
        </div>

        $if(app.multiHost)$
        <h5 class="subheader">Hosts</h5>
        <ul class="side-nav">
            <li id="hs_all" class="host-select"><a id="hn_all" class="host-nav" name="all" href="#">All</a></li>
            $app.hostList:{host |
            <li id="hs_$host.name$" class="host-select"><a id="hn_$host.name$" class="host-nav" name="$host.name$"
                                                           href="#">$host.name$</a></li>
            }$
        </ul>
        $endif$

    </div>
    <div id="content" class="large-9 columns">
        <div class="row">
            <div class="large-12 columns">
                <h5 class="subheader">
                    <span id="app-title" class="subheader"></span>&nbsp;&raquo;
                    $metric.name$ <span id="gloading"><img src="$constants.imageBase$/ajax-loader4.gif"/></span>
                </h5>
                <h6 class="subheader">
                    <span class="subheader" id="range-detail" class="subheader"></span>
                    &nbsp;&nbsp;<a title="Previous range" href="" id="range-back"><i class="fi-rewind"></i></a>
                    &nbsp;&nbsp;<a title="Next range" href="" id="range-forward"><i class="fi-fast-forward"></i></a>
                    &nbsp;&nbsp;|&nbsp;&nbsp;<a href="" title="Share these graphs" id="range-permalink"><i
                        class="fi-link"></i></a>
                    &nbsp;&nbsp;<a href="" title="JSON API" id="json-link"><i class="fi-cloud"></i></a>
                </h6>
            </div>
        </div>
        <div class="row" id="missing_graph">
            <div class="large-12 columns">
                <h6>No Data</h6>
                <img src="$constants.imageBase$/missing-data.png"/>
            </div>
        </div>
        $graphs:{graph |
        <div class="row" id="gr_$graph.field$">
            <div class="large-12 columns">
                $metric_page(index, app, metric, graph)$
            </div>
        </div>
        }$
    </div>
</div>
$footer()$
<script type="application/javascript">
    jQuery(document).foundation();
    bindFieldChecks();
    bindTopBar('$index$');
    jQuery('#gloading').hide();
    jQuery('#missing_graph').hide();

    $graphs:{
        graph |
        $if(!graph.active)
        $
        jQuery('#gr_$graph.field$').hide();
        $endif$
    }
    $

    var downsampleFn = 'avg';

    $downsampleList:{
        df |
        $if(df.active)
        $
        downsampleFn = '$df.functionName$';
        $endif$
    }
    $

    var dataConfig = {};
    dataConfig.index = '$index$';
    dataConfig.app = '$app.name$';
    dataConfig.name = '$metric.name$';
    dataConfig.downsampleFn = downsampleFn;
    dataConfig.tz = '';
    dataConfig.range = '$range.name$'; //Range always has the "closest" name...even if only an interval was specified.
    dataConfig.downsampleInterval = downsampleInterval(dataConfig.range);
    dataConfig.startTimestamp = $range.startMillis$; //Echo if input timestamps...
    dataConfig.endTimestamp = $range.endMillis$;
    if (dataConfig.startTimestamp == 0 || dataConfig.endTimestamp == 0) {
        dataConfig.endTimestamp = new Date().getTime();
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
    }

    $if(host)
    $
    dataConfig.host = '$host$';
    activateHost(dataConfig.host);
    $else$
    dataConfig.host = '';
    $endif$

    function render(data) {
        if (data.length > 0) {
            jQuery('#missing_graph').hide();
            $graphs:{
                graph |
                loadGraph(data, $graph.asJSON$, dataConfig);
            }
            $
            showCheckedFields();
        } else {
            hideCheckedFields();
            jQuery('#missing_graph').show();
        }
        jQuery('#gloading').hide();
    }

    activateRange(dataConfig.range);
    loadMetricData(dataConfig, render);

    jQuery('.range-nav').click(function (event) {
        event.preventDefault();
        var selectedRange = jQuery(this).attr('name');
        dataConfig.range = selectedRange;
        dataConfig.downsampleInterval = downsampleInterval(selectedRange);
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
        activateRange(selectedRange);
        loadMetricData(dataConfig, render);
    });

    jQuery('.dfs').click(function (event) {
        var fn = jQuery('input[name=dsfield]:checked').val();
        dataConfig.downsampleFn = fn;
        loadMetricData(dataConfig, render);
    });

    jQuery('#range-back').click(function (event) {
        event.preventDefault();
        dataConfig.endTimestamp = dataConfig.startTimestamp;
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
        loadMetricData(dataConfig, render);
    });

    jQuery('#range-forward').click(function (event) {
        event.preventDefault();

        if (dataConfig.endTimestamp < (new Date().getTime() - 60000)) {
            dataConfig.startTimestamp = dataConfig.endTimestamp;
            dataConfig.endTimestamp = dataConfig.startTimestamp + rangeMillis(dataConfig.range);
            loadMetricData(dataConfig, render);
        }
    });

    $if(app.multiHost)
    $
    if (dataConfig.host == '') activateHost('all');
    jQuery('.host-nav').click(function (event) {
        event.preventDefault();
        var host = jQuery(this).attr('name');
        if (host == 'all') {
            dataConfig.host = '';
        } else {
            dataConfig.host = host;
        }
        activateHost(host);
        loadMetricData(dataConfig, render);
    });
    $endif$

    var currTimeField = jQuery('#curr-time');
    var currTimeFormat = 'YYYY-MM-DD HH:mm:ss Z';

    $if(zoneList)
    $
    $zoneList:{
        zone |
        if (offsetMatchesLocal('$zone.id$')) setCurrentTimezone('$zone.display$');
    }
    $
    jQuery('.zone-nav').click(function (event) {
        event.preventDefault();
        var zn = jQuery(this);
        var zid = zn.attr('name');
        dataConfig.tz = zid;
        currTimeField.text(moment().tz(zid).format(currTimeFormat));
        setCurrentTimezone(zn.text());
        loadMetricData(dataConfig, render);
    });
    $endif$

    currTimeField.text(moment().format(currTimeFormat));

    /*
     function update() {
     loadMetricData('$app.name$','$metric.name$', render);
     }
     setInterval(update, 20000); */
</script>
$modals()$
</body>
</html>
>>