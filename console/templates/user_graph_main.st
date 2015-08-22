user_graph_main(uid, index, indexList, downsampleList, zoneList, graph) ::= <<
<!doctype html>
<html class="no-js" lang="en">
$head()$
<body>
$topbar(index, indexList, zoneList, uid)$
<div class="row">
    <div id="sidebar" class="large-3 columns">
        <h5 class="subheader">Range</h5>
        <ul class="side-nav">
            <li id="rs_hour" class="range-select"><a id="rn_hour" class="range-nav" name="hour" href="#">Hour</a></li>
            <li id="rs_day" class="range-select"><a id="rn_day" class="range-nav" name="day" href="#">Day</a></li>
            <li id="rs_week" class="range-select"><a id="rn_week" class="range-nav" name="week"  href="#">Week</a></li>
            <li id="rs_month" class="range-select"><a id="rn_month" class="range-nav" name="month"  href="#">Month</a></li>
        </ul>

        $if(graph.rate)$
        <h5 class="subheader">Rate Unit</h5>
        <div class="gfselect">
            <span class="nowrap"><input class="rselect" name="rselectfield" value="perSecond" id="rselect_sec" type="radio" checked="checked"><label for="rselect_sec">Per Second</label></span>
            <span class="nowrap"><input class="rselect" name="rselectfield" value="perMinute" id="rselect_min" type="radio"><label for="rselect_min">Per Minute</label></span>
            <span class="nowrap"><input class="rselect" name="rselectfield" value="perHour" id="rselect_hour" type="radio"><label for="rselect_hour">Per Hour</label></span>
        </div>
        $endif$

        <h5 class="subheader">Downsample With</h5>
        <div class="gfselect">
        $downsampleList:{downsampleFn |
            <span class="nowrap">
                $if(downsampleFn.active)$
                <input class="dfs" type="radio" checked="checked" name="dsfield" value="$downsampleFn.functionName$" id="dsfield_$downsampleFn.functionName$">
                $else$
                <input class="dfs" type="radio" name="dsfield" value="$downsampleFn.functionName$" id="dsfield_$downsampleFn.functionName$">
                $endif$
                <label for="dsfield_$downsampleFn.functionName$">$downsampleFn.displayName$</label>
            </span>
        }$
        </div>
    </div>
    <div id="content" class="large-9 columns">
        <div class="row">
            <div class="large-12 columns">
                <h4>$graph.title$</h4>
                <h4 class="subheader">$graph.description$</h4>
                <h5 class="subheader">
                    <span id="app-title" class="subheader"></span>&nbsp;&raquo;
                    $graph.key.name$&nbsp;&raquo;&nbsp;$graph.key.field$ <span id="gloading"><img src="$constants.imageBase$/ajax-loader4.gif"/></span>
                </h5>
                <h6 class="subheader">
                    <span class="subheader" id="range-detail"></span>
                    &nbsp;&nbsp;<a title="Previous range" href="" id="range-back"><i class="fi-rewind"></i></a>
                    &nbsp;&nbsp;<a title="Next range" href="" id="range-forward"><i class="fi-fast-forward"></i></a>
                    &nbsp;&nbsp;|&nbsp;&nbsp;<a href="" title="Share these graphs" id="range-permalink"><i class="fi-link"></i></a>
                    &nbsp;&nbsp;<a href="" title="JSON API" id="json-link"><i class="fi-cloud"></i></a>
                    &nbsp;&nbsp;<a title="Edit this graph" href="" id="graph-edit"><i class="fi-page-edit"></i></a>
                </h6>
                <h6 class="subheader">$graph.yLabel$</h6>
            </div>
        </div>
        <div class="row" id="missing_graph">
            <div class="large-12 columns">
                <h6>No Data</h6>
                <img src="$constants.imageBase$/missing-data.png"/>
            </div>
        </div>
        <div class="row" id="gr_$graph.key.field$">
            <div class="large-12 columns" id="main_graph">
            </div>
        </div>
        <div class="row" id="grfs_$graph.key.field$">
            <div class="large-12 columns" id="main_graph_stats">
            </div>
        </div>
    </div>
</div>
<script type="application/javascript">
    jQuery(document).foundation();
    bindTopBar('$index$');
    jQuery('#gloading').hide();
    jQuery('#missing_graph').hide();

    var downsampleFn = 'avg';

    $downsampleList:{df |
      $if(df.active)$
        downsampleFn = '$df.functionName$';
      $endif$
    }$

    var config = {};
    config.field = '$graph.key.field$';
    config.x_label = '$graph.yLabel$';
    config.y_label = '';
    config.left_margin = 60;
    config.bottom_margin = 75;

    var dataConfig = {};

    dataConfig.target = '#main_graph';
    dataConfig.height = 450;
    dataConfig.width = 950;

    dataConfig.index = '$index$';
    dataConfig.app = '$graph.key.application$';
    dataConfig.name = '$graph.key.name$';
    dataConfig.downsampleFn = '$graph.downsampleFn$';
    dataConfig.tz = '';
    dataConfig.range = '$graph.range$';
    dataConfig.downsampleInterval = downsampleInterval(dataConfig.range);
    dataConfig.startTimestamp = $graph.startTimestamp$;
    dataConfig.endTimestamp = $graph.endTimestamp$;
    if(dataConfig.startTimestamp == 0 || dataConfig.endTimestamp == 0) {
        dataConfig.endTimestamp = new Date().getTime();
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
    }

    dataConfig.host = '$graph.key.host$';
    dataConfig.t = 'user_graph_stats';

    function render(data) {
        if(data.length > 0) {
            jQuery('#missing_graph').hide();
            jQuery(dataConfig.target).show();
            jQuery('#main_graph_stats').show();
            loadGraph(data, config, dataConfig);
            renderFieldStats(dataConfig, '$graph.key.field$', '#main_graph_stats');
        } else {
            jQuery('#missing_graph').show();
            jQuery(dataConfig.target).hide();
            jQuery('#main_graph_stats').hide();
        }
        jQuery('#gloading').hide();
    }

    activateRange(dataConfig.range);
    loadMetricData(dataConfig, render);

    jQuery('.range-nav').click(function(event) {
        event.preventDefault();
        var selectedRange = jQuery(this).attr('name');
        dataConfig.range = selectedRange;
        dataConfig.downsampleInterval = downsampleInterval(selectedRange);
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
        activateRange(selectedRange);
        loadMetricData(dataConfig, render);
    });

    jQuery('.dfs').click(function(event) {
        dataConfig.downsampleFn = jQuery('input[name=dsfield]:checked').val();
        loadMetricData(dataConfig, render);
    });

    dataConfig.rateUnit = 'perSecond';

    $if(graph.rate)$
    switch('$graph.rateUnit$'.toLowerCase()) {
        case "perminute":
            jQuery("#rselect_min").prop("checked", true);
            dataConfig.rateUnit = 'perMinute';
            break;
        case "perhour":
            jQuery("#rselect_hour").prop("checked", true);
            dataConfig.rateUnit = 'perHour';
            break
    }

    jQuery('.rselect').click(function(event) {
        dataConfig.rateUnit = jQuery('input[name=rselectfield]:checked').val();
        loadMetricData(dataConfig, render);
    });
    $endif$

    jQuery('#range-back').click(function(event){
        event.preventDefault();
        dataConfig.endTimestamp = dataConfig.startTimestamp;
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
        loadMetricData(dataConfig, render);
    });

    jQuery('#range-forward').click(function(event){
        event.preventDefault();

        if(dataConfig.endTimestamp < (new Date().getTime() - 60000)) {
            dataConfig.startTimestamp = dataConfig.endTimestamp;
            dataConfig.endTimestamp = dataConfig.startTimestamp + rangeMillis(dataConfig.range);
            loadMetricData(dataConfig, render);
        }
    });

    function triggerFieldSave(event) {
        event.preventDefault();
        showGraphSave(dataConfig, '$graph.key.field$');
    }
    jQuery('#graph-edit').click(triggerFieldSave);

   var currTimeField = jQuery('#curr-time');
   var currTimeFormat = 'YYYY-MM-DD HH:mm:ss Z';

   $if(zoneList)$
    $zoneList:{zone |
    if(offsetMatchesLocal('$zone.id$')) setCurrentTimezone('$zone.display$');
    }$
    jQuery('.zone-nav').click(function(event) {
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

</script>
$modals()$
</body>
</html>
>>