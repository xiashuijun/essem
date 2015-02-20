user_graph_stats(index, app, key, range, stats) ::= <<
<table width="950">
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