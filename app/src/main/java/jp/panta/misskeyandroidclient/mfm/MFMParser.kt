package jp.panta.misskeyandroidclient.mfm

import net.pantasystem.milktea.model.emoji.Emoji
import java.net.URLDecoder
import java.util.regex.Matcher
import java.util.regex.Pattern

object MFMParser{

    fun parse(text: String?, emojis: List<Emoji>? = emptyList()): Root?{
        text?: return null
        //println("textSize:${text.length}")
        val root = Root(text)
        NodeParser(text, root,
            emojis?.map{
                it.name to it
            }?.toMap()?: emptyMap()
        ).parse()
        return root
    }


    /**
     * @param start 担当する文字列のスタート地点
     * @param end 担当する文字列の終了地点
     * @param parent 親ノードこのParserはこの parentの内側の処理をしていることになる
     * つまりNodeParserとの関係はparent : NodeParserという一対一の関係になる。
     * <parent>child content</parent>
     *         ↑start       ↑end
     */
    class NodeParser(
        private val sourceText: String,
        val parent: Node,
        private val emojiNameMap: Map<String, Emoji>,
        val start: Int = parent.insideStart,
        val end: Int = parent.insideEnd
    ){
        // タグ探索開始
        // タグ探索中
        // タグ探索完了
        // タグ探索キャンセル

        private var position: Int = start

        /**
         * 一番最後にタグを検出したタグの最後のpositionがここに代入される。
         * recoveryBeforeText()されるときに使用される
         */
        private var finallyDetected: Int = start


        /**
         * 第一段階としてタグの先頭の文字に該当するかを検証する
         * キャンセルされたときはここからやり直される
         */
        private val parserMap: Map<Char, List<()-> Element?>> = mapOf(
            '<' to listOf(::parseBlock), //斜体、小文字、中央揃え、横伸縮、左右反転、回転、飛び跳ねる
            '~' to listOf(::parseStrike), //打消し線
            //'(' to ::parseExpansion, //横伸縮
            '`' to listOf(::parseCode), //コード
            '>' to listOf(::parseQuote), //引用
            '*' to listOf(::parseTypeStar),  // 横伸縮対称揺れ, 太字
            '【' to listOf(::parseTitle),//タイトル
            '[' to listOf(::parseSearch, ::parseLink, ::parseTitle),
            '?' to listOf(::parseLink),
            'S' to listOf(::parseSearch),
            ':' to listOf(::parseEmoji),
            '@' to listOf(::parseMention),
            '#' to listOf(::parseHashTag),
            'h' to listOf(::parseUrl)

        )

        /**
         * 何にも該当しない文字を葉として追加する
         * @param tagStart 次に存在するNodeの始点
         * text<Node> この場合だと4になる
         */
        private fun recoveryBeforeText(tagStart: Int){
            // 文字数が０より多いとき
            if((tagStart - finallyDetected) > 0){
                val text = sourceText.substring(finallyDetected, tagStart)
                parent.childElements.add(Text(text, finallyDetected))
            }
        }

        fun parse(){
            while(position < end){
                val parser = parserMap[sourceText[position]]

                if(parser == null){
                    // 何にも該当しない場合は繰り上げる
                    position ++
                }else{

                    val node = parser.mapNotNull {
                        it.invoke()
                    }.firstOrNull()
                    // nodeが実際に存在したとき
                    if(node != null){

                        // positionは基本的にはNodeの開始地点のままなので発見したNodeの終了地点にする
                        position = node.end


                        // Nodeの直前のNodeに含まれないLeafの回収作業を行う
                        recoveryBeforeText(node.start)

                        // 新たに発見したnodeの一番最後の外側の文字を記録する
                        finallyDetected = node.end



                        // 発見したNodeを追加する
                        parent.childElements.add(node)


                        // 新たに発見した子NodeのためにNodeParserを作成する
                        // 新たに発見した子Nodeの内側を捜索するのでparentは新たに発見した子Nodeになる
                        if(node is Node){
                            NodeParser(sourceText, parent = node, emojiNameMap = emojiNameMap).parse()
                        }


                    }else{
                        position ++
                    }
                }
            }
            //parent.endTag.start == position -> true
            recoveryBeforeText(parent.insideEnd)

        }

        /**
         * タグの開始位置や終了位置、内部要素の開始、終了位置は正規表現とMatcherを利用し現在のポジションと合わせ相対的に求める
         */

        private fun parseTypeStar(): Node?{
            val boldPattern = Pattern.compile("""\A\*\*(.+?)\*\*""", Pattern.DOTALL)
            val animationPattern = Pattern.compile("""\A\*\*\*(.+?)\*\*\*""", Pattern.DOTALL)
            val currentInside = sourceText.substring(position, parent.insideEnd)

            if(animationPattern.matcher(currentInside).find()){
                // アニメーションはサポートしていないため終了
                return null
            }

            val matcher = boldPattern.matcher(currentInside)
            if(!matcher.find()){
                return null
            }
            if(parent.elementType.elementClass.weight < ElementType.BOLD.elementClass.weight || parent.elementType == ElementType.BOLD){
                return null
            }


            return Node(
                start = position,
                end = position + matcher.end(),
                insideStart = position + 2,
                insideEnd = position + matcher.end() - 2,
                elementType = ElementType.BOLD
            )
        }

        private fun parseBlock(): Node?{
            val pattern = Pattern.compile("""\A<([a-z]+)>(.+?)</\1>""", Pattern.DOTALL)
            val matcher = pattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find()){
                return null
            }else{
                val tagName = matcher.group(1)?: return null

                val tag = MFMContract.blockTypeTagNameMap[tagName]?: return null

                // Parentより自分のほうが重い又は同じタグの場合無効
                if(parent.elementType.elementClass.weight < tag.elementClass.weight || parent.elementType == tag){
                    return null
                }

                return Node(
                    start = position,
                    end = position + matcher.end(),
                    insideStart = position + tagName.length + 2,
                    insideEnd = position + matcher.end(2),
                    elementType = tag
                )


            }
        }

        private fun parseStrike(): Node?{
            val pattern = Pattern.compile("""\A~~(.+?)~~""", Pattern.DOTALL)
            val matcher = pattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find()){
                return null
            }
            if(parent.elementType.elementClass.weight < ElementType.STRIKE.elementClass.weight || parent.elementType == ElementType.STRIKE){
                return null
            }
            val child = matcher.group(1)
                ?: return null
            return Node(
                start = position,
                end = position + matcher.end(),
                insideStart = position + matcher.start(1),
                insideEnd = position + matcher.start(1) + child.length,
                elementType = ElementType.STRIKE
            )
        }



        private fun parseCode(): Node?{
            val pattern = Pattern.compile("""\A```(.*)```""", Pattern.DOTALL)
            val matcher = pattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find()){
                return null
            }
            if(parent.elementType != ElementType.ROOT){
                return null
            }
            return Node(
                start = position,
                end = position + matcher.end(),
                insideStart = position + matcher.start(1),
                insideEnd = position + matcher.end(1),
                elementType = ElementType.CODE
            )
        }

        private fun parseQuote(): Node?{

            if(position > 0){
                val c = sourceText[ position - 1 ]
                // 直前の文字が改行コードではないかつ、親が引用コードではない
                if( (c != '\r' && c != '\n') && parent.elementType != ElementType.QUOTE){
                    println("直前の文字が改行コードではないかつ、親が引用コードではない")
                    return null
                }
                if( parent.elementType.elementClass.weight < ElementType.QUOTE.elementClass.weight && parent.elementType != ElementType.ROOT){
                    println("親ノードのほうが小さい")
                    return null
                }
            }
            val quotePattern = Pattern.compile("""^>(?:[ ]?)([^\n\r]+)(\n\r|\n)?""", Pattern.MULTILINE)
            val matcher = quotePattern.matcher(sourceText.substring(position, parent.insideEnd))


            if(!matcher.find()){
                return null
            }
            val nodeEnd = matcher.end()
            println(matcher.group(1))


            // > の後に何もない場合キャンセルする
            if(nodeEnd + position <= position){
                return null
            }
            ///println("inside:$inside")

            return Node(
                start = position,
                end = nodeEnd + position,
                insideStart = position + 1,
                insideEnd = position + nodeEnd, // >を排除する
                elementType = ElementType.QUOTE
            )
        }

        private val titlePattern = Pattern.compile("""\A[【\[]([^\n\[\]【】]+)[】\]](\n|\z)""")

        private val searchPattern = Pattern.compile("""^(.+?)[ |　]((?i)Search|検索|\[検索]|\[Search])$""", Pattern.MULTILINE)
        private val linkPattern = Pattern.compile("""\??\[(.+?)]\((https?|ftp)(://[-_.!~*'()a-zA-Z0-9;/?:@&=+${'$'},%#]+)\)""")

        private fun parseTitle(): Node?{
            if(position > 0){
                val c = sourceText[ position - 1 ]
                // 直前の文字が改行コードではないかつ、親が引用コードではない
                if( (c != '\r' && c != '\n') ){
                    return null
                }
                if( parent.elementType != ElementType.ROOT){
                    return null
                }
            }
            val matcher = titlePattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find()){
                return null
            }
            if(parent.elementType != ElementType.ROOT){
                return null
            }
            return Node(
                start = position,
                end = position + matcher.end(),
                insideStart = position + matcher.start(1),
                insideEnd = position + matcher.end(1),
                elementType = ElementType.TITLE
            )
        }

        /**
         * 他の要素と違いタグの要素の検出ポイントより以前から要素が始まっているので現在のポイントより前を検索することになる。
         * 要素が見つかれば他の要素のルールと同じく要素の末端が次のポジションになる
         * 戻るポイントは基本的には検索済みであるが要素は発見されなかった場所なので無視してしまうリスクは限りなくゼロに近い。
         */
        private fun parseSearch(): Search?{

            val targetText = sourceText.substring(finallyDetected, parent.insideEnd)
            val matcher = searchPattern.matcher(targetText)
            if(!matcher.find() || parent.elementType != ElementType.ROOT){
                return null
            }

            val text = matcher.group(1)?: return null
            return Search(
                text = text,
                start = matcher.start(),
                end = matcher.end(),
                insideStart = matcher.start(1),
                insideEnd = matcher.end(1)
            )
        }


        private fun parseLink(): Link?{

            val targetText = sourceText.substring(position, parent.insideEnd)
            val matcher = linkPattern.matcher(targetText)
            if(!matcher.find()){
                return null
            }

            if(parent.elementType.elementClass.weight <= ElementClass.LINK.weight){
                return null
            }

            val text = matcher.group(1)?: return null
            val url = matcher.group(2)?: return null
            return Link(
                text = text,
                start = position + matcher.start(),
                end = position + matcher.end(),
                insideStart = position + matcher.start(1),
                insideEnd = position + matcher.end(1),
                url = url + matcher.group(3)
            )
        }

        private val emojiPattern = Pattern.compile("""\A:([a-zA-Z0-9+\-_]+):""")
        private fun parseEmoji(): EmojiElement?{
            val matcher = emojiPattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find() || parent.elementType.elementClass.weight <= ElementType.EMOJI.elementClass.weight){
                return null
            }
            val tagName = matcher.group(1)?: return null
            val emoji: Emoji = emojiNameMap[tagName]
                ?: return null

            return EmojiElement(
                emoji,
                tagName,
                start = position + matcher.start(),
                end = position + matcher.end(),
                insideStart = position + matcher.start(1),
                insideEnd = position + matcher.end(1)
            )
        }

        private val mentionPattern = Pattern.compile("""\A@([\w._\-]+)(@[\w._\-]+)?""")
        private fun parseMention(): Mention?{
            if(!beforeSpaceCheck()){
                return null
            }
            val matcher = mentionPattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find() || parent.elementType.elementClass.weight <= ElementType.MENTION.elementClass.weight){
                return null
            }
            return Mention(
                position + matcher.start(),
                position + matcher.end(),
                position + matcher.start(),
                position + matcher.end(),
                text = matcher.group(),
                host = matcher.nullableGroup(2)
            )
        }

        private val hashTagPattern = Pattern.compile("""#[^\s.,!?'"#:/\[\]【】@]+""")
        private fun parseHashTag(): HashTag?{
            if(!beforeSpaceCheck()){
                return null
            }
            val matcher = hashTagPattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find() || parent.elementType.elementClass.weight <= ElementType.MENTION.elementClass.weight){
                return null
            }

            return HashTag(
                position + matcher.start(),
                position + matcher.end(),
                position + matcher.start(),
                position + matcher.end(),
                matcher.group()
            )

        }

        private val urlPattern = Pattern.compile("""(https?)(://)([-_.!~*'()\[\]a-zA-Z0-9;/?:@&=+${'$'},%#]+)""")
        private fun parseUrl(): Link?{
            val matcher = urlPattern.matcher(sourceText.substring(position, parent.insideEnd))
            if(!matcher.find()){
                return null
            }
            fun decodeUrl(url: String): String{
                return URLDecoder.decode(url.replace("%20", "+"), "UTF-8")
            }
            return if(matcher.nullableGroup(1) == "http"){
                Link(
                    decodeUrl(matcher.group()),
                    position + matcher.start(),
                    position + matcher.end(),
                    position + matcher.start(),
                    position + matcher.end(),
                    matcher.group()
                )
            }else {
                Link(
                    decodeUrl(matcher.nullableGroup(3)?: matcher.group()),
                    position + matcher.start(),
                    position + matcher.end(),
                    position + (matcher.nullableStart(3)?: matcher.start()),
                    position + (matcher.nullableEnd(3)?: matcher.end()),
                    url = matcher.group()
                )
            }
        }

        private val spaceCRLFPattern = Pattern.compile("""\s""")
        private fun beforeSpaceCheck(): Boolean{
            return position <= parent.insideStart || spaceCRLFPattern.matcher(sourceText[ position - 1].toString()).find()

        }

    }

    private fun Matcher.nullableGroup(group: Int): String?{
        return try{
            this.group(group)
        }catch(e: Exception){
            null
        }
    }

    private fun Matcher.nullableStart(group: Int): Int?{
        return try{
            this.start(group)
        }catch(e: Exception){
            null
        }
    }

    private fun Matcher.nullableEnd(group: Int): Int?{
        return try{
            this.end(group)
        }catch(e: Exception){
            null
        }
    }

}