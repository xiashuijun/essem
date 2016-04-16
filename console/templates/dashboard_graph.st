dashboard_graph(index, graph, dash) ::= <<
function render_$graph.sid$() {
 var config_$graph.sid$ = {};
 config_$graph.sid$.field = '$graph.key.field$';
 config_$graph.sid$.with_labels = true;
 config_$graph.sid$.with_title = $dash.withTitles$;
 config_$graph.sid$.title = '$graph.title$';
 config_$graph.sid$.x_label = '$graph.yLabel$';
 config_$graph.sid$.y_label = '';

 config_$graph.sid$.left_margin = 60;
 config_$graph.sid$.bottom_margin = 75;
 config_$graph.sid$.right_margin = 75;
 config_$graph.sid$.full_width = true;
 config_$graph.sid$.full_height = false;
 config_$graph.sid$.small_text = true;

 var dataConfig_$graph.sid$ = {};
 dataConfig_$graph.sid$.target = '#gd_$graph.sid$';

 $if(dash.customWidth)$
 dataConfig_$graph.sid$.width = $dash.width$;
 $else$
 dataConfig_$graph.sid$.width = 950;
 $endif$

 $if(dash.customHeight)$
 dataConfig_$graph.sid$.height = $dash.height$;
 $else$
 dataConfig_$graph.sid$.height = 450;
 $endif$

 dataConfig_$graph.sid$.index = '$index$';

 dataConfig_$graph.sid$.app = '$graph.key.application$';
 dataConfig_$graph.sid$.name = '$graph.key.name$';
 dataConfig_$graph.sid$.downsampleFn = '$graph.downsampleFn$';

 $if(dash.tz)$
 dataConfig_$graph.sid$.tz = '$dash.tz$';
 $else$
 dataConfig_$graph.sid$.tz = '';
 $endif$

 dataConfig_$graph.sid$.range = '$graph.range$';
 dataConfig_$graph.sid$.downsampleInterval = downsampleInterval(dataConfig_$graph.sid$.range);

 dataConfig_$graph.sid$.startTimestamp = $graph.startTimestamp$;
 dataConfig_$graph.sid$.endTimestamp = $graph.endTimestamp$;
 if(dataConfig_$graph.sid$.startTimestamp == 0 || dataConfig_$graph.sid$.endTimestamp == 0) {
  dataConfig_$graph.sid$.endTimestamp = new Date().getTime();
  dataConfig_$graph.sid$.startTimestamp = dataConfig_$graph.sid$.endTimestamp - rangeMillis(dataConfig_$graph.sid$.range);
 }

 $if(graph.rateUnit)$
 dataConfig_$graph.sid$.rateUnit = '$graph.rateUnit$';
 $endif$

 dataConfig_$graph.sid$.host = '$graph.key.host$';

 function render_$graph.sid$(data) {
     if(data.length > 0) {
         loadGraph(data, config_$graph.sid$, dataConfig_$graph.sid$);
     } else {
         //TODO
     }
     //jQuery('#gloading').hide();
 }
 loadMetricData(dataConfig_$graph.sid$, render_$graph.sid$);
}
>>