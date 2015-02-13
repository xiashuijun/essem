field_stats(index, app, key, range, stats) ::= <<
<table width="99%">
    $if(key.hasHost)$
    <caption>$app$&nbsp;&raquo;$key.host$&nbsp;&raquo;&nbsp;$key.name$
        &nbsp;&raquo;&nbsp;<span id="fstats-field-title"></span>
        <br/><span id="fstats-range-detail"></span>
    </caption>
    $else$
    <caption>$app$&nbsp;&raquo;&nbsp;$key.name$
        &nbsp;&raquo;&nbsp;<span id="fstats-field-title"></span>
        <br/><span id="fstats-range-detail"></span>
    </caption>
    $endif$
    <tr>
        <th>Count</th>
        <th>Min</th>
        <th>Max</th>
        <th>Avg</th>
        <th>Std</th>
    </tr>
    <tr>
        <td>$stats.count$</td>
        <td>$stats.formattedMin$</td>
        <td>$stats.formattedMax$</td>
        <td>$stats.formattedAvg$</td>
        <td>$stats.formattedStd$</td>
    </tr>
</table>
>>