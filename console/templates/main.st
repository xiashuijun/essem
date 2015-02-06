main(index, indexList, appList, defaultApp, zoneList, time, content) ::= <<
<!doctype html>
<html class="no-js" lang="en">
$head()$
<body>
$topbar(index, indexList, zoneList)$
<div class="row">
    <div id="sidebar" class="large-3 columns">
        <h5 class="subheader">Applications</h5>
        $apps(index, appList)$
    </div>
    <div id="content" class="large-9 columns">
        <div class="row collapse">
            <h5 class="subheader">Metrics</h5>

            <div class="large-11 columns">
                <input id="mf" type="text" placeholder="Just type &trade;"/>
            </div>
            <div class="large-1 column" id="mf-clear"><span class="postfix"><i id="mf-clear-x" class="fi-x-circle"></i></span>
            </div>
        </div>
        <div class="row">
            <div class="large-12 columns">
                <label class="l_filter">Type</label>
                <input checked="checked" class="mfc" type="radio" name="tf" value="all" id="mt_all"><label for="mt_all">All</label>
                <input class="mfc" type="radio" name="tf" value="timer" id="mt_timer"><label
                    for="mt_timer">Timers</label>
                <input class="mfc" type="radio" name="tf" value="meter" id="mt_meter"><label
                    for="mt_meter">Meters</label>
                <input class="mfc" type="radio" name="tf" value="gauge" id="mt_gauge"><label
                    for="mt_gauge">Gauges</label>
                <input class="mfc" type="radio" name="tf" value="counter" id="mt_counter"><label for="mt_counter">Counters</label>
                <input class="mfc" type="radio" name="tf" value="histogram" id="mt_histogram"><label for="mt_histogram">Histograms</label>
            </div>
        </div>
        <div class="row">
            <div class="large-12 columns">
                <label class="l_filter">Activity</label>
                <input checked="checked" class="mfc" type="radio" name="af" value="all" id="ma_all"><label for="ma_all">All</label>
                <input class="mfc" type="radio" name="af" value="active" id="ma_active"><label
                    for="ma_active">Active</label>
                <input class="mfc" type="radio" name="af" value="boring" id="ma_boring"><label
                    for="ma_boring">Boring</label>
            </div>
        </div>
        <div class="row">
            <div id="subcontent" class="large-12 columns">
            </div>
        </div>
    </div>
</div>
$footer()$
<script type="application/javascript">
    jQuery(document).foundation();
    jQuery(document).ready(function () {
        bindApps('$index$');
        bindTopBar('$index$');
        loadMetrics('$index$', '$defaultApp$');
        bindMetricsFilter('$index$', '$defaultApp$');
        activateApp('$defaultApp$');
        var currTimeField = jQuery('#curr-time');
        var currTimeFormat = 'YYYY-MM-DD HH:mm:ss Z';
        currTimeField.text(moment().format(currTimeFormat));
    });
</script>
$modals()$
</body>
</html>
>>
