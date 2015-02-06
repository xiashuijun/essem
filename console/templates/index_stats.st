index_stats(index, stats) ::= <<
<table width="85%">
    <caption class="capitalize">$index$ Index Status</caption>
    <tr>
        <th>Metric Records</th>
        <td>$stats.docCount$</td>
    </tr>
    <tr>
        <th>On-Disk Storage (MB)</th>
        <td>$stats.storedMB$</td>
    </tr>

    <tr>
        <th>In-Memory Field Data (KB)</th>
        <td>$stats.fieldDataKB$</td>
    </tr>

    <tr>
        <th>Segment Count</th>
        <td>$stats.segmentCount$</td>
    </tr>

    <tr>
        <th>Segment Memory (KB)</th>
        <td>$stats.segmentMemoryKB$</td>
    </tr>
</table>

<table width="85%">
    <caption>ES Cluster &ndash; Heap Status</caption>
    <tr>
        <th>Used (MB)</th>
        <td>$stats.clusterHeapUsedMB$</td>
    </tr>
    <tr>
        <th>Max (MB)</th>
        <td>$stats.clusterHeapMaxMB$</td>
    </tr>

    <tr>
        <th>Percent Used</th>
        <td>$stats.clusterPercentHeapUsed$</td>
    </tr>
</table>

<table width="85%">
    <caption>Local Heap Status</caption>
    <tr>
        <th>Used (MB)</th>
        <td>$stats.heapUsedMB$</td>
    </tr>
    <tr>
        <th>Max (MB)</th>
        <td>$stats.heapMaxMB$</td>
    </tr>

    <tr>
        <th>Percent Used</th>
        <td>$stats.percentHeapUsed$</td>
    </tr>
</table>

>>