dashboard_graph(index, graph, tz, width, height) ::= <<
var config_$graph.id$ = {};
config_$graph.id$.field = '$graph.key.field$';
config_$graph.id$x_label = '$graph.yLabel$';
config_$graph.id$.y_label = '';

config_$graph.id$.left_margin = 60;
config_$graph.id$.bottom_margin = 75;
config_$graph.id$.right_margin = 75;

var dataConfig_$graph.id$ = {};
dataConfig_$graph.id$.target = '#gd_$graph.id$';

$if(width)$
dataConfig_$graph.id$.width = $width$;
$else$
dataConfig_$graph.id$.width = 950;
$endif$

$if(height)$
dataConfig_$graph.id$.height = $height$;
$else$
dataConfig_$graph.id$.height = 450;
$endif$

dataConfig_$graph.id$.index = '$index$';

dataConfig_$graph.id$.app = '$graph.key.application$';
dataConfig_$graph.id$.name = '$graph.key.name$';
dataConfig_$graph.id$.downsampleFn = '$graph.downsampleFn$';

$if(tz)$
dataConfig_$graph.id$.tz = '$tz$';
$else$
dataConfig_$graph.id$.tz = '';
$endif$

dataConfig_$graph.id$.range = '$graph.range$';
dataConfig_$graph.id$.downsampleInterval = downsampleInterval(dataConfig_$graph.id$.range);

dataConfig_$graph.id$.startTimestamp = $graph.startTimestamp$;
dataConfig_$graph.id$.endTimestamp = $graph.endTimestamp$;
if(dataConfig_$graph.id$.startTimestamp == 0 || dataConfig_$graph.id$.endTimestamp == 0) {
 dataConfig_$graph.id$.endTimestamp = new Date().getTime();
 dataConfig_$graph.id$.startTimestamp = dataConfig_$graph.id$.endTimestamp - rangeMillis(dataConfig_$graph.id$.range);
}

dataConfig_$graph.id$.host = '$graph.key.host$';

function render_$graph.id$(data) {
    if(data.length > 0) {
        loadGraph(data, config_$graph.id$, dataConfig_$graph.id$);
    } else {
        //TODO
    }
    //jQuery('#gloading').hide();
}

loadMetricData(dataConfig_$graph.id$, render_$graph.id$);
>>