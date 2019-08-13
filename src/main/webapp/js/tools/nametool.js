define(['util', 'dialog', 'icheck'], function (util, dialog) {
    var nametool = {};
    var modul = 'translate';
    var apis = {
        loadConfigNames: '/file/manager/simpleConfigNames',
        writeConfig: '/file/manager/writeConfig',
        readConfig: '/file/manager/readConfig',
        translate: '/translate/translate',
        mutiTranslate:'/translate/mutiTranslate'
    }

    nametool.init = function () {
        bindEvents();

        $('#resultconfig input[type=radio],#resultconfig input[type=checkbox]').iCheck({
            checkboxClass: 'icheckbox_square-green',
            radioClass: 'iradio_square-green'
        });
        reloadBizs();
        return this;
    }

    function reloadBizs(biz) {
        util.requestData(apis.loadConfigNames, {modul:modul},function (bizs) {
            $('#bizs').empty();
            for (var i = 0; i < bizs.length; i++) {
                $('#bizs').append('<option value="' + bizs[i] + '">' + bizs[i] + '</option>');
            }
            $('#bizs').val(biz);
            $('#bizs').change();
        });
    }

    function query() {
        var text = $('input[name=chinese]').val().trim();
        var biz = $('#bizs').val();
        var englishs = [];
        $('input[name=translate]:checked').each(function () {
            englishs.push($(this).val());
        });
        if (englishs.length == 0) {
            layer.msg('必选一个英语翻译获取命名效果')
            return;
        }
        var index = layer.load(1, {
            shade: [0.1,'#fff']
        });
        let beginTime = new Date().getTime();
        util.requestData(apis.translate, {biz: biz, orginChars: text, englishs: englishs}, function (results) {
            $('#result').val(results.join('\n'));

            $('#namerequest>b').text('取名耗时: ' + (new Date().getTime() - beginTime) + ' ms');
            layer.close(index);
        },function () {
            layer.close(index);
        });
    }

    $('input[name=chinese]').bind('keydown', function (event) {
        var event = event || window.event;
        if (event.keyCode == 13)
            query();//查询函数
    });

    function bindEvents() {
        var events = [{selector: '#plusBiz', types: ['click'], handler: plusBiz},
            {selector: '#editBizConfig', types: ['click'], handler: editBizConfig},
            {selector: '#enname', types: ['click'], handler: translate},
            {selector: '#bizs', types: ['change'], handler: loadConfig},
            {selector: '#muticolumntranslate', types: ['click'], handler: muticolumntranslate},
            {selector: '#confirmPrefix', types: ['click'], handler: addPrefix}];
        util.regPageEvents(events);

        /**
         * 添加前缀
         */
        function addPrefix() {
            var prefix = $('#mutiltranslate').find('input[name=prefix]').val().trim();
            var oldValue = $('#mutiltranslate').find('textarea[name=right]').val();
            if(oldValue){
                var oldValues = oldValue.split('\n');
                var newValues = [];
                for(var i=0;i<oldValues.length;i++){
                    newValues.push(prefix+' '+oldValues[i] + ';');
                }
                $('#mutiltranslate').find('textarea[name=right]').val(newValues.join('\n'));
            }
        }

        /**
         * 多列翻译;打开对话框
         */
        function muticolumntranslate() {
            dialog.create('多列翻译')
                .setContent($('#mutiltranslate'))
                .setWidthHeight('90%', '90%')
                .addBtn({type:'yes',text:'确定',handler:callMutilTranslate})
                .build();

            /**
             * 调用多列翻译
             */
            function callMutilTranslate() {
                var left = $('#mutiltranslate').find('textarea[name=left]').val().trim();
                var splits = left.split('\n');
                var values = [];
                for(var i=0;i<splits.length;i++){
                    if(!splits[i] || splits[i] == '' || splits[i].trim() == ''){
                        continue;
                    }
                    values.push(splits[i].trim());
                }

                //去掉空行
                $('#mutiltranslate').find('textarea[name=left]').val(values.join('\n'));
                var index = layer.load(1, {
				  shade: [0.1,'#fff']
				});
                try{
                    util.requestData(apis.mutiTranslate,{words:values},function (results) {
                         $('#mutiltranslate').find('textarea[name=right]').val(results.join('\n'));
                         layer.close(index);
                    });
                }catch (e) {
                    layer.close(index);
                }
            }
        }

        /**
         * 加载映射配置
         */
        function loadConfig() {
            var biz = $(this).val();
            util.requestData(apis.readConfig, {baseName: biz,modul:modul}, function (configs) {
                $('#bizmapping').val(configs);
            });
        }

        /**
         * 翻译框内中文
         */
        function translate() {
            query();
        }

        /**
         * 打开新业务对话框
         */
        function plusBiz() {
            dialog.create('新业务')
                .setContent($('#newbiz'))
                .setWidthHeight('500px', '80%')
                .addBtn({
                    type: 'yes', text: '确定', handler: function (index, layero) {
                        var params = util.serialize2Json($('#newbiz>form').serialize());
                        if (!params.biz || !params.content) {
                            layer.msg('请把信息填写完整');
                            return;
                        }
                        params.modul = modul;
                        util.requestData(apis.writeConfig, params, function () {
                            reloadBizs(params.biz);
                            layer.close(index);
                        });
                    }
                }).build();
        }

        /**
         * 编辑业务配置
         */
        function editBizConfig() {
            var biz = $('#bizs').val();
            var configs = $('#bizmapping').val().trim();

            util.requestData(apis.writeConfig, {biz: biz, content: configs,modul:modul});
        }

    }

    return nametool.init();
});