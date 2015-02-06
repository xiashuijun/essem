metrics_filter(index, app) ::= <<
<div class="row">
    <div class="large-12 columns">
        <dl class="sub-nav">
            <dd id="fm_all" class="fm active"><a class="fmc" href="#" name="all">All</a></dd>
            <dd id="fm_timer" class="fm"><a class="fmc" href="#" name="timer">Timers</a></dd>
            <dd id="fm_meter" class="fm"><a class="fmc" href="#" name="meter">Meters</a></dd>
            <dd id="fm_gauge" class="fm"><a class="fmc" href="#" name="gauge">Gauges</a></dd>
            <dd id="fm_counter" class="fm"><a class="fmc" href="#" name="counter">Counters</a></dd>
            <dd id="fm_histogram" class="fm"><a class="fmc" href="#" name="histogram">Histograms</a></dd>
        </dl>
    </div>
</div>

<script type="application/javascript">bindMetricsFilter('$index$', '$app$');</script>
>>