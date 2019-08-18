define(['util','template'],function (util,template) {
    var novel = {};

    novel.init = function () {
        $('#netsource').bind('change',function () {
            novel.netsource = $(this).val();
        });

        //加载所有网源
        util.requestData('/novel/listNetSources',function (netsources) {
            $('#netsource').empty();
            for(var key in netsources){
                $('#netsource').append('<option value="'+key+'">'+key +'-'+netsources[key].url+'</option>')
            }

            //选取第一个网源
            novel.netsource = $('#netsource').val();
        });

        $('#search>input[name=keyword]').bind('keydown',function (event) {
            var event = event || window.event;
            if(event.keyCode == 13)
                query();//查询函数
        });

        //搜书按扭
        $('#search>.input-group-btn>button').bind('click',query);

        //加载最近看过的书
        util.requestData('/novel/latestUse',function (novels) {
            $('#lastvisted').empty();
            for(var i=0;i<novels.length;i++){
                var novel = novels[i];
                $('#lastvisted').append('<li class="visted-item" netSource="'+novel.netSource+'" bookId="'+novel.bookId+'" chapterUrl="'+novel.chapterUrl+'" bookname="'+novel.name+'" ><a href="javascript:;">'+novel.name+'['+novel.netSource+']</a></li>');
            }
        });

        // 绑定图片和书目录点击事件
        $('#result').on('click','li>.bookimg>a',showChapter);
        $('#result').on('click','li>.bookinfo>.bookname>a',showChapter);
        $('#lastvisted').on('click','li>a',showChapter);

        function showChapter() {
            var $parentLi = $(this).closest('li');
            var bookId = $parentLi.attr('bookId');
            var chapterUrl = $parentLi.attr('chapterUrl');
            var bookname = $parentLi.attr('bookname');
            var netSource = $parentLi.attr('netSource');
            if(!netSource){
                netSource = novel.netsource;
            }

            util.tab('/app/novel/chaptercatalog.html',{bookId:bookId,chapterUrl:chapterUrl,netSource:netSource,bookname:bookname});
        }
        return this;
    };

    function query() {
        var input = $('#search>input').val().trim();
        if(!input){
            return ;
        }
        var index = layer.load(1, {
            shade: [0.1,'#fff']
        });
        util.requestData('/novel/searchBook', {netSource: novel.netsource, keyword: input}, function (novels) {
            var $result = $('#result').empty();
            $('#resultCount').text(novels.length);
            for (var i = 0; i < novels.length; i++) {
                var currentNovel = novels[i];
                var novelHtml = template('novelTemplate', currentNovel);

                $result.append(novelHtml);
            }

            layer.close(index);
        },function () {
            layer.close(index);
        });
    }

    return novel.init();
});