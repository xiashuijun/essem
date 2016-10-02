histogram_main(uid, index, indexList, zoneList, host, range, app, graph) ::= <<
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
        <h5 class="subheader">Units</h5>
        <div class="gfselect">
            <span class="nowrap"><input class="uselect" name="uselectfield" value="nanos" id="uselect_nanos" type="radio"><label for="uselect_nanos">Nanos</label></span>
            <span class="nowrap"><input class="uselect" name="uselectfield" value="micros" id="uselect_micros" type="radio" checked="checked"><label for="uselect_micros">Micros</label></span>
            <span class="nowrap"><input class="uselect" name="uselectfield" value="millis" id="uselect_millis" type="radio"><label for="uselect_millis">Millis</label></span>
            <span class="nowrap"><input class="uselect" name="uselectfield" value="seconds" id="uselect_seconds" type="radio"><label for="uselect_seconds">Seconds</label></span>
        </div>
        $if(app.multiHost)$
        <h5 class="subheader">Hosts</h5>
        <ul class="side-nav">
            <li id="hs_all" class="host-select"><a id="hn_all" class="host-nav" name="all" href="#">All</a></li>
            $app.hostList:{host |
            <li id="hs_$host.name$" class="host-select"><a id="hn_$host.name$" class="host-nav" name="$host.name$" href="#">$host.name$</a></li>
            }$
        </ul>
        $endif$
    </div>
    <div id="content" class="large-9 columns">
        <div class="row">
            <div class="large-12 columns">
                <h4>$graph.title$</h4>
                <h4 class="subheader">$graph.description$</h4>
                <h5 class="subheader">
                    <span id="app-title" class="subheader"></span>&nbsp;&raquo;
                    $graph.metric.name$&nbsp;&raquo;&nbsp;$graph.field$ <span id="gloading"><img src="$constants.imageBase$/ajax-loader4.gif"/></span>
                </h5>
                <h6 class="subheader">
                    <span class="subheader" id="range-detail"></span>
                    &nbsp;&nbsp;<a title="Previous range" href="" id="range-back"><i class="fi-rewind"></i></a>
                    &nbsp;&nbsp;<a title="Next range" href="" id="range-forward"><i class="fi-fast-forward"></i></a>
                    &nbsp;&nbsp;|&nbsp;&nbsp;<a href="" title="Share these graphs" id="range-permalink"><i class="fi-link"></i></a>
                    &nbsp;&nbsp;<a href="" title="JSON API" id="json-link"><i class="fi-cloud"></i></a>
                    &nbsp;&nbsp;<a title="Edit this graph" href="" id="graph-edit"><i class="fi-page-edit"></i></a>
                </h6>
                <h6 class="subheader">$graph.labels.y$</h6>
            </div>
        </div>
        <div class="row" id="missing_graph">
            <div class="large-12 columns">
                <h6>No Data</h6>
                <img src="$constants.imageBase$/missing-data.png"/>
            </div>
        </div>
        <div class="row" id="gr_$graph.field$">
            <div class="large-12 columns" id="main_graph">
            </div>
        </div>
        <div class="row" id="grfs_$graph.field$">
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

    var config = {};
    config.field = '$graph.field$';
    config.x_label = '$graph.labels.y$';
    config.y_label = '[todo]';
    config.left_margin = 60;
    config.bottom_margin = 75;
    config.units = 'millis';

    var dataConfig = {};

    dataConfig.target = '#main_graph';
    dataConfig.height = 450;
    dataConfig.width = 950;

    dataConfig.index = '$index$';
    dataConfig.app = '$app.name$';
    dataConfig.name = '$graph.metric.name$';

    dataConfig.timeUnit = 'micros';
    dataConfig.tz = '';
    dataConfig.range = '$range.name$';
    dataConfig.startTimestamp = $range.startMillis$;
    dataConfig.endTimestamp = $range.endMillis$;
    if(dataConfig.startTimestamp == 0 || dataConfig.endTimestamp == 0) {
        dataConfig.endTimestamp = new Date().getTime();
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
    }

    $if(host)$
    dataConfig.host = '$host$';
    activateHost(dataConfig.host);
    $else$
    dataConfig.host = '';
    $endif$

    dataConfig.t = 'user_graph_stats';

    function render(data) {
        if(data.length > 0) {
            jQuery('#missing_graph').hide();
            jQuery(dataConfig.target).show();
            jQuery('#main_graph_stats').show();
            loadHistogram(data, config, dataConfig);
            //renderFieldStats(dataConfig, '$graph.key.field$', '#main_graph_stats');
        } else {
            jQuery('#missing_graph').show();
            jQuery(dataConfig.target).hide();
            jQuery('#main_graph_stats').hide();
        }
        jQuery('#gloading').hide();
    }

    loadHistogramData(dataConfig, render);

    jQuery('.range-nav').click(function(event) {
        event.preventDefault();
        var selectedRange = jQuery(this).attr('name');
        dataConfig.range = selectedRange;
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
        activateRange(selectedRange);
        loadHistogramData(dataConfig, render);
    });

    jQuery('.uselect').click(function(event) {
        dataConfig.timeUnit = jQuery('input[name=uselectfield]:checked').val();
        loadHistogramData(dataConfig, render);
    });

    jQuery('#range-back').click(function(event){
        event.preventDefault();
        dataConfig.endTimestamp = dataConfig.startTimestamp;
        dataConfig.startTimestamp = dataConfig.endTimestamp - rangeMillis(dataConfig.range);
        loadHistogramData(dataConfig, render);
    });

    jQuery('#range-forward').click(function(event){
        event.preventDefault();

        if(dataConfig.endTimestamp < (new Date().getTime() - 60000)) {
            dataConfig.startTimestamp = dataConfig.endTimestamp;
            dataConfig.endTimestamp = dataConfig.startTimestamp + rangeMillis(dataConfig.range);
            loadHistogramData(dataConfig, render);
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
        loadHistogramData(dataConfig, render);
    });
   $endif$

    currTimeField.text(moment().format(currTimeFormat));

</script>
$modals()$
</body>
</html>
>>