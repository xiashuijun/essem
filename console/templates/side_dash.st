side_dash(index, dashList) ::= <<
<ul class="side-nav">
$dashList:{dash |
    <li class="app-select">
        <a target="_blank" href="/console/$index$/dash?tag=$dash$">$dash$</a>
    </li>
}$
</ul>
>>