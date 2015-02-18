metric_page(index, app, metric, graphs) ::= <<
$graphs:{graph |
<div class="row">
    <div class="large-12 columns">
        <h6>$graph.title$ &nbsp;&nbsp;
            <a href="#" title="$graph.shortTitle$" id="fstats_$graph.field$"><i class="fi-info"></i></a>
            &nbsp;&nbsp;<a href="#" title="$graph.shortTitle$" id="fsave_$graph.field$"><i class="fi-paperclip"></i></a>
        </h6>
    </div>
</div>
<div class="row">
    <div id="m_$metric.nameHash$_$graph.field$" class="large-12 columns"></div>
</div>
}$
>>