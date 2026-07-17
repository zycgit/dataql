<template>
  <splitpanes class="dataway-split-pane" :horizontal="split === 'horizontal'" @resized="handleResize">
    <pane :min-size="minPercent" :size="defaultPercent">
      <div class="dataway-split-pane__content">
        <slot name="paneL" />
      </div>
    </pane>
    <pane :min-size="minPercent" :size="100 - defaultPercent">
      <div class="dataway-split-pane__content">
        <slot name="paneR" />
      </div>
    </pane>
  </splitpanes>
</template>

<script>
import {Splitpanes, Pane} from 'splitpanes';
import 'splitpanes/dist/splitpanes.css';

export default {
    name: 'SplitPane',
    components: {
        Splitpanes,
        Pane
    },
    props: {
        minPercent: {
            type: Number,
            default: 0
        },
        defaultPercent: {
            type: Number,
            default: 50
        },
        split: {
            type: String,
            default: 'vertical'
        }
    },
    emits: ['resize'],
    methods: {
        handleResize(payload) {
            const panes = Array.isArray(payload) ? payload : payload.panes;
            const firstPane = Array.isArray(panes) ? panes[0] : null;
            if (firstPane && firstPane.size !== undefined) {
                this.$emit('resize', firstPane.size);
            }
        }
    }
};
</script>

<style>
.dataway-split-pane,
.splitpanes {
  width: 100%;
  height: 100%;
}

.dataway-split-pane__content {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.splitpanes__splitter {
  background-color: #dcdfe6;
  box-sizing: border-box;
  position: relative;
}

.splitpanes--vertical > .splitpanes__splitter {
  width: 5px;
  cursor: col-resize;
}

.splitpanes--horizontal > .splitpanes__splitter {
  height: 5px;
  cursor: row-resize;
}
</style>
