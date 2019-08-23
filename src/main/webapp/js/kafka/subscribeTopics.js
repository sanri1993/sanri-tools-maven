define(['util','dialog','icheck'],function(util,dialog){
    var subscribeTopics = {};

    var apis = {
        groupSubscribeTopics:'/kafka/groupSubscribeTopics',
        autoSelectMonitor:'/kafka/groupSubscribeTopicsMonitor',
        groupTopicMonitor:'/kafka/groupTopicMonitor',
        editOffset:'/kafka/editOffset',
        nearbyDatas:'/kafka/nearbyDatas',
        lastDatas:'/kafka/lastDatas',
        serializes:'/zk/serializes'
    }

    subscribeTopics.init = function () {
        bindEvents();
        //获取当前分组
        var parseUrl = util.parseUrl();
        subscribeTopics.group = parseUrl.params.group;
        subscribeTopics.conn = parseUrl.params.name;
        $('#groupnav').text(subscribeTopics.conn+'->'+subscribeTopics.group);
        
        loadSubscribeTopics();
        return this;
    }
    
    function loadSubscribeTopics() {
        var index = layer.load(1, {
          shade: [0.1,'#fff']
        });
        util.requestData(apis.autoSelectMonitor,{group:subscribeTopics.group,clusterName:subscribeTopics.conn},function(data) {
            var $tbody = $('#topictable>tbody').empty();
            for (var i = 0; i < data.length; i++) {
                var topicItem = data[i];
                var $tr = $('<tr><td>' + (i + 1) + '</td><td>' + topicItem.topic + '</td><td>' + topicItem.partitions + '</td><td>' + topicItem.offset + '</td><td>' + topicItem.logSize + '</td><td>' + topicItem.lag + '</td><td><a href="javascript:void(0);"topic="' + topicItem.topic + '">监控</a></td></tr>').appendTo($tbody);
            }
            layer.close(index);
        },function () {
            layer.close(index);
        });
    }

    function bindEvents() {
        var events = [
            {parent:'#topictable',selector:'a',types:['click'],handler:topicDetail},
            {selector:'button[name=refreshNow]',types:['click'],handler:refreshTopicOffset},
            {parent:'#monitorOffset',selector:'button[name=offsetMin]',types:['click'],handler:setOffsetMin},
            {parent:'#monitorOffset',selector:'button[name=showdata]',types:['click'],handler:loadSerializes},
            {parent:'#monitorOffset',selector:'button[name=lastdata]',types:['click'],handler:loadSerializes},
            {selector:'#serializeTools',types:['change'],handler:showdata},
            {parent:'#datadetail',selector:'button',types:['click'],handler:jsonView}
        ];
        util.regPageEvents(events);

        function jsonView() {
            var offset = $(this).closest('tr').attr('offset');
            var json = $(this).parent().siblings('td:last').text();
            // $('#jsonViewLoad').text(json);

            dialog.create('offset:'+offset +' 的数据')
                .setWidthHeight('500px','500px')
                .setContent($('#jsonView'))
                .onOpen(loadJsonData)
                .build();

            function loadJsonData() {
                require(['jsonview'],function () {
                    $('#jsonViewLoad').JSONView(json);
                });

            }
        }

        /**
         * 附近数据展示
         */
        function showdata() {
            //获取暂存数据
            var $form = $('#serializeTools').closest('form');
            var btnName = $form.data('btnName');
            var partition = $form.data('partition');
            var offset = $form.data('offset');

            //获取公共数据
            var conn = subscribeTopics.conn;
            var topic = $('#monitorOffset').data('topic');

            //获取选择数据
            var serialize = $(this).val();

            //调用接口选择
            var switchApi = apis.nearbyDatas;
            if(btnName == 'lastdata'){
                switchApi = apis.lastDatas;
            }

            util.requestData(switchApi,{clusterName:conn,topic:topic,partition:partition,offset:offset,serialize:serialize},function (datas) {
               // $('#datadetail').html(data.join('<br/>'));
                var $tbody = $('#datadetail').find('tbody').empty();
                for(var offset in datas){
                    var btn = '<button type="button" class="btn btn-sm btn-primary"><i class="fa fa-book"></i> JSON </button>';
                    $tbody.append('<tr offset="'+offset+'"><td>'+btn+'</td><td>'+offset+'</td><td>'+datas[offset]+'</td></tr>');
                }
            });
        }
        
        function loadSerializes() {
            //使用 form 来记住部分数据
            var $tr = $(this).closest('tr');
            var btnName = $(this).attr('name');
            var partition = $tr.attr('partition');
            var offset = $tr.data('topicInfo')['offset'];

            var $form = $('#serializeTools').closest('form');
            $form.data('btnName',btnName);
            $form.data('partition',partition);
            $form.data('offset',offset);

            var conn = subscribeTopics.conn;
            var topic = $('#monitorOffset').data('topic');

            //加载序列化工具列表
            util.requestData(apis.serializes,function (serializes) {
                $('#serializeTools').empty();
                for(var i=0;i<serializes.length;i++){
                    $('#serializeTools').append('<option value="'+serializes[i]+'">'+serializes[i]+'</option>');
                }

                $('#serializeTools').val('').change();

                var buildDialog = dialog.create('显示topic['+topic+']partition['+partition+']offset['+offset+']附近['+btnName+']的数据')
                    .setWidthHeight('90%','90%')
                    .setContent($('#showdataDialog'));
                buildDialog.build();
            });
        }
        
        function setOffsetMin() {
            var $tr = $(this).closest('tr');

            var conn = subscribeTopics.conn;
            var group = subscribeTopics.group;
            var topic = $('#monitorOffset').data('topic');
            var partition = $tr.attr('partition');
            var offset = $tr.find('input').val().trim();
            if(!offset){
                layer.msg('填写 offset 值');
                return ;
            }
            util.requestData(apis.editOffset,{name:conn,group:group,topic:topic,partition:partition,offset:offset});
        }
        
        function topicDetail() {
            var topic = $(this).attr('topic');

            var buildDialog=dialog.create('监控主题['+topic+']')
                .setContent($('#monitorOffset'))
                .onClose(offsetMonitorClear)
                .setWidthHeight('80%','90%');

            //写入当前主题监控数据
            $('#monitorOffset').find('input[name=interval]').val(60000);
            $('#monitorOffset').data('topic',topic);
            $('#monitorOffset').data('group',subscribeTopics.group);
            $('#monitorOffset').data('conn',subscribeTopics.conn);

            buildDialog.build();

            refreshTopicOffset();       //初次调用
            buildDialog.timer = setInterval(refreshTopicOffset,60000);

            function offsetMonitorClear() {
                window.clearInterval(buildDialog.timer);
            }
        }

        function refreshTopicOffset() {
            var $tbody = $('#monitorOffset').find('tbody').empty();
            var topic = $('#monitorOffset').data('topic');

            var index = layer.load(1, {
              shade: [0.1,'#fff']
            });
            util.requestData(apis.groupTopicMonitor,{group:subscribeTopics.group,topic:topic,clusterName:subscribeTopics.conn},function(data){
                for(var i=0;i<data.length;i++){
                    var offsetShow = data[i];
                    var lastUpdateTime = util.FormatUtil.dateFormat(offsetShow.modified,'yyyy-MM-dd HH:mm:ss');
                    var $btnGroup = '<div class="btn-group btn-group-sm"><button class="btn btn-sm btn-success" name="showdata">附近数据</button><button class="btn btn-sm btn-warning" name="lastdata">尾部数据</button></div>';
                    var $setOffset = '<div class="input-group"><input type="text"class="form-control input-sm " name=""offsetMin" placeholder="设置值" value= "'+offsetShow.minOffset+'" name="offsetMin"><span class="input-group-btn"><button class="btn btn-danger btn-sm "name="offsetMin">设置</button></span></div>';
                    var $tr = $('<tr partition="'+offsetShow.partition+'"><td>'+offsetShow.topic+'</td><td>'+offsetShow.partition+'</td><td>'+offsetShow.offset+'</td><td>'+offsetShow.logSize+'</td><td>'+offsetShow.lag+'</td><td>'+lastUpdateTime+'</td><td>'+offsetShow.minOffset+'</td><td class="col-xs-3 col-lg-2">'+$setOffset+'</td><td>'+$btnGroup+'</td></tr>')
                        .appendTo($tbody);
                    $tr.data('topicInfo',offsetShow);
                }

                layer.close(index);
            },function () {
                layer.close(index);
            });
        }
        
        function topicMonitorClick() {
            
        }
    }

    return subscribeTopics.init();
});