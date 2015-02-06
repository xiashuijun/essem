metric_page(index, app, metric, graphs) ::= <<
$graphs:{graph |
<div class="row">
    <div class="large 12 columns">
        <h6>$graph.title$</h6>
    </div>
</div>
<div class="row">
    <div id="m_$metric.nameHash$_$graph.field$" class="large 12 columns"></div>
</div>
}$
>>