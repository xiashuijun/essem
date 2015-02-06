topbar(index, indexList, zoneList) ::= <<
<nav class="top-bar" data-topbar data-options="is_hover: false">
    <ul class="title-area">

        <li class="name">
            <h1>
                <a href="/console/$index$"><img src="$constants.imageBase$/eicon.png"/></a>
            </h1>
        </li>
        <li class="toggle-topbar menu-icon"><a href="#"><span>Menu</span></a></li>
    </ul>
    <section class="top-bar-section">

        <ul class="right">
            <li class="divider"></li>
            <li class="has-dropdown">
                <a href="#">System</a>
                <ul class="dropdown">
                    <li><a id="topbar_status" href="#">Status</a></li>
                </ul>
            </li>
            <li class="divider"></li>
            <li class="has-dropdown">
                <a href="#">Help</a>
                <ul class="dropdown">
                    <li><a href="#">Documentation</a></li>
                    <li class="divider"></li>
                    <li><a href="#">About Essem</a></li>
                </ul>
            </li>
            <li class="divider"></li>
            <li><a href="" id="curr-time"></a></li>
            <li class="divider"></li>
        </ul>

        <ul class="left">
            <li class="divider"></li>
            <li class="has-dropdown">
                <a class="active" href="#"><span class="capitalize">$first(indexList)$</span></a>
                <ul class="dropdown">
                    $rest(indexList):{allowedIndex |
                    <li><a href="/console/$allowedIndex$" class=""><span class="capitalize">$allowedIndex$</span></a>
                    </li>
                    }$
                </ul>
            </li>
            $if(zoneList)$
            <li class="divider"></li>
            <li class="has-dropdown">
                <a id="set_tz" href="#"></a>
                <ul class="dropdown">
                    $zoneList:{zone |
                    <li id="tzs_$i$" class="zone-select"><a id="tzn_$i$" class="zone-nav" name="$zone.id$" href="#">$zone.display$</a>
                    </li>
                    }$
                </ul>
            </li>
            <li class="divider"></li>
            $endif$

        </ul>
    </section>
</nav>
>>