dashboard(uid, index, title, tags, graphs, withTitles, withStats, tz, width, height, grid, sbg, mbg, lbg) ::= <<
<!doctype html>
<html class="no-js" lang="en">
$head()$
<body>
<div class="row" $if(grid)$style="min-width:95% !important;"$endif$>
    <div id="content" class="large-12 columns">
        $if(grid)$
         <ul class="small-block-grid-$sbg$ medium-block-grid-$mbg$ large-block-grid-$lbg$">
         $graphs:{graph |
            <li><div id="gd_$graph.id$"></div></li>
         }$
         </ul>
        $else$
         $graphs:{graph |
            <div class="row"><div id="gd_$graph.id$" class="large-12 columns"></div></div>
         }$
        $endif$
    </div>
</div>
<script type="application/javascript">
    jQuery(document).foundation();
    $graphs:{graph |
      $dashboard_graph(index, graph, tz, width, height)$;
    }$
</script>
</body>
</html>
>>