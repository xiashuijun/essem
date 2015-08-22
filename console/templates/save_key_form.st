save_key_form(graph) ::= <<
<form action="POST" id="save-key-form">
    <div class="row">
        <div class="small-12 colunms">
            <h6 class="subheader" id="save-key-form-title">
                <h5>$graph.key.captionWithField$</h5>
            </h6>
        </div>
    </div>
    <div class="row collapse">
        <div class="small-7 columns">
            <label><b>Title</b></label>
            <input id="skey-title" type="text" name="title" value="$graph.title$"/>

            <label><b>Description</b></label>
            <input id="skey-description" type="text" name="description" value="$graph.description$"/>

            <label><b>Label</b></label>
            <input id="skey-ylabel" type="text" name="yLabel" value="$graph.yLabel$"/>

            <label><b>Tags</b></label>
            <input id="skey-tags" type="text" name="tagString" value="$graph.tagString$"/>

        </div>
        <div class="small-2 columns">
            <input type="submit" class="button postfix" value="Save"/>
        </div>
    </div>
    <input type="hidden" name="application" value="$graph.key.application$"/>
    <input type="hidden" name="host" value="$graph.key.host$"/>
    <input type="hidden" name="instance" value="$graph.key.instance$"/>
    <input type="hidden" name="name" value="$graph.key.name$"/>
    <input type="hidden" name="field" value="$graph.key.field$"/>
    <input type="hidden" name="downsampleFn" value="$graph.downsampleFn$"/>
    <input type="hidden" name="rateUnit" value="$graph.rateUnit$"/>
    <input type="hidden" name="id" value="$graph.id$"/>
    <input type="hidden" name="range" value="$graph.range$"/>
    <input type="hidden" name="startTimestamp" value="$graph.startTimestamp$"/>
    <input type="hidden" name="endTimestamp" value="$graph.endTimestamp$"/>
</form>
>>