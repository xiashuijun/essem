field_stats(index, app, key, range, stats) ::= <<
<table width="99%">
    <caption>$key.caption$&nbsp;&raquo;&nbsp;<span id="fstats-field-title"></span>
        <br/><span id="fstats-range-detail"></span>
    </caption>
    <thead>
     <tr>
        <th>Count</th>
        <th>Min</th>
        <th>Max</th>
        <th>Avg</th>
        <th>Std</th>
     </tr>
    </thead>
    <tbody>
     <tr>
        <td>$stats.count$</td>
        <td>$stats.formattedMin$</td>
        <td>$stats.formattedMax$</td>
        <td>$stats.formattedAvg$</td>
        <td>$stats.formattedStd$</td>
     </tr>
    </tbody>
</table>
>>