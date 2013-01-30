package kara.styles

import java.util.ArrayList
import java.util.HashMap
import kara.styles.*
import kara.util.join

trait Selector {
    fun toExternalForm() : String
}

trait SelectorTrait {
    fun toExternalForm() : String
}

object EmptyTrait : SelectorTrait {
    override fun toExternalForm(): String {
        return ""
    }
}

public trait StyleClass : SelectorTrait, Selector {
    fun name() : String

    override fun toExternalForm(): String {
        return ".${name()}"
    }
}
public enum class PseudoClass : StyleClass {
    root firstChild lastChild firstOfType lastOfType onlyChild onlyOfType
    empty link visited active focus hover target enabled disabled checked

    override fun toExternalForm(): String {
        return ":${name()}"
    }
}

/**
 * Represents a single stylesheet element.
 */
open class Element() {
    val children : MutableList<StyledElement> = ArrayList<StyledElement>()
    val attributes = HashMap<String, Any>()

    public inner class IdSelector(val name : String) : SelectorTrait, Selector {
        fun invoke(body : StyledElement.() -> Unit) {
            any.invoke(this, body)
        }

        override fun toExternalForm(): String {
            return "#$name"
        }
    }

    public inner open class TagSelector(val name : String) : Selector {
        fun id(name: String) : Selector = invoke(IdSelector(name))
        fun id(name : String, body : StyledElement.() -> Unit) = invoke(IdSelector(name), body)
        fun id(id : IdSelector, body : StyledElement.() -> Unit) = invoke(id, body)

        // TODO: When KT-3300 is fixed, make klass a vararg to allow multiple clases
        fun c(klass : StyleClass, body : StyledElement.() -> Unit) {
            invoke(klass, body)
        }
        fun c(klass : StyleClass) : Selector = invoke(klass)

        fun invoke(body : StyledElement.() -> Unit) {
            invoke(EmptyTrait, body)
        }

        // TODO: varargs fail due to http://youtrack.jetbrains.com/issue/KT-3300
        fun invoke(traits : Array<SelectorTrait>, body : StyledElement.() -> Unit) {
            s(SimpleSelector(this, traits).toExternalForm(), body)
        }

        fun invoke(t: SelectorTrait, body : StyledElement.() -> Unit) {
            invoke(array(t), body)
        }

        fun invoke(t: SelectorTrait) : Selector {
            return invoke(array(t))
        }

        fun invoke(t: Array<SelectorTrait>) : Selector {
            return SimpleSelector(this, t)
        }

        fun isAny() : Boolean = "*" == name

        override fun toExternalForm(): String {
            return name
        }
    }

    val any = TagSelector("*")
    val a = TagSelector("a")
    val b = TagSelector("b")
    val body = TagSelector("body")
    val button = TagSelector("button")
    val canvas = TagSelector("canvas")
    val div = TagSelector("div")
    val em = TagSelector("em")
    val fieldset = TagSelector("fieldset")
    val form = TagSelector("form")
    val h1 = TagSelector("h1")
    val h2 = TagSelector("h2")
    val h3 = TagSelector("h3")
    val h4 = TagSelector("h4")
    val h5 = TagSelector("h5")
    val img = TagSelector("img")
    val input = TagSelector("input")
    val label = TagSelector("label")
    val ol = TagSelector("ol")
    val p = TagSelector("p")
    val select = TagSelector("select")
    val span = TagSelector("span")
    val strong = TagSelector("strong")
    val table = TagSelector("table")
    val textarea = TagSelector("textarea")
    val ul = TagSelector("ul")
    val li = TagSelector("li")
    val option = TagSelector("option")
    val optgroup = TagSelector("optgroup")
    val tr = TagSelector("tr")
    val tbody = TagSelector("tbody")
    val td = TagSelector("td")
    val th = TagSelector("th")

    public fun id(name: String, body : StyledElement.() -> Unit) {
        any.id(name, body)
    }

    //TODO: Return IdSelector instead of SelectorTrait when KT-3301 is fixed
    public fun id(name: String) : SelectorTrait = IdSelector(name)

    fun c(klass : StyleClass, body : StyledElement.() -> Unit) {
        any.invoke(klass, body)
    }

    public fun att(name : String) : Attribute = Attribute(name, HasAttribute(name))

    public class Attribute internal (val name : String, val filter : AttFilter) : SelectorTrait {
        public fun startsWith(value : String) : Attribute {
            return Attribute(name, StartsWith(value))
        }

        public fun equalTo(value : String) : Attribute {
            return Attribute(name, Equals(value))
        }

        public fun endsWith(value : String) : Attribute {
            return Attribute(name, EndsWith(value))
        }

        public fun contains(value : String) : Attribute {
            return Attribute(name, Contains(value, AttributeValueTokenizer.Substring))
        }

        public fun containsInHypen(value : String) : Attribute {
            return Attribute(name, Contains(value, AttributeValueTokenizer.Hypen))
        }

        public fun containsInSpaces(value : String) : Attribute {
            return Attribute(name, Contains(value, AttributeValueTokenizer.Spaces))
        }

        override fun toExternalForm(): String {
            return "[$name${filter.toExternalForm()}]"
        }
    }

    class SimpleSelector(val tag : TagSelector, val traits : Array<SelectorTrait>) : Selector {
        override fun toExternalForm() : String {
            val answer = StringBuilder()

            val isAny = tag.isAny()
            if (!isAny) {
                answer.append(tag.name)
            }

            for (t in traits) {
                answer.append(t.toExternalForm())
            }

            if (answer.length == 0 && isAny) {
                return "*"
            }

            return answer.toString()
        }
    }

    //TODO: vararg
    public fun forAny(selectors : Array<Selector>, body : StyledElement.() -> Unit) {
        s(UnionSelector(selectors).toExternalForm(), body)
    }

    public fun forAny(selectors : Array<Selector>) : UnionSelector {
        return UnionSelector(selectors)
    }

    class UnionSelector(val selectors : Array<Selector>) : Selector {
        override fun toExternalForm(): String {
            return selectors.map { it.toExternalForm() } join ","
        }
    }

    /**
     * Creates a new child element with the given selector and block.
     */
    fun s(selector : String, init : StyledElement.() -> Unit) {
        val element = StyledElement(selector)
        element.init()
        children.add(element)
    }
}


class StyledElement(val selector : String) : Element() {
    /**
     * Writes the element to the builder with the given indenation.
     */
    fun build(builder : StringBuilder, baseSelector : String) {
        if (baseSelector.length() > 0)
            builder.append("$baseSelector $selector {\n")
        else
            builder.append("$selector {\n")
        for (a in attributes.keySet()) {
            val attr = attributes[a]!!
            builder.append("    $a: ${attr.toString()};\n")
        }
        builder.append("}\n")
        val childBaseSelector = if (baseSelector.length() > 0) "$baseSelector $selector" else selector
        for (val child in children) {
            child.build(builder, childBaseSelector)
        }
    }

    /** Strongly-typed method for pulling attributes out of the hash. */
    fun getAttribute<T>(name : String) : T {
        if (attributes.containsKey(name))
            return attributes[name] as T
        else
            throw Exception("Element has no attribute ${name}")
    }

    /** Strongly-typed method for pulling attributes out of the hash, with a default return value. */
    fun getAttribute<T>(name : String, default : T) : T {
        if (attributes.containsKey(name))
            return attributes[name] as T
        else
            return default
    }

    /** Shorthand for making a color inside a stylesheet. */
    fun c(colorString : String) : Color {
        return color(colorString)
    }

    var backgroundAttachment : BackgroundAttachment?
        get() = getAttribute<BackgroundAttachment>("background-attachment")
        set(value) {
            attributes["background-attachment"] = value.toString()
        }

    var backgroundColor : Color?
        get() = getAttribute<Color>("background-color")
        set(value) {
            attributes["background-color"] = value.toString()
        }

    var backgroundImage : String?
        get() = getAttribute<String>("background-image")
        set(value) {
            attributes["background-image"] = value.toString()
        }

    var backgroundPosition : String?
        get() = getAttribute<String>("background-position")
        set(value) {
            attributes["background-position"] = value.toString()
        }

    var backgroundRepeat : BackgroundRepeat?
        get() = getAttribute<BackgroundRepeat>("background-repeat")
        set(value) {
            attributes["background-repeat"] = value.toString()
        }

    var border : String = ""
        set(value) {
            val tokens = value.split(' ')
            for (val token in tokens) {
                if (isLinearDimension(token))
                    borderWidth = LinearDimension.fromString(token)
                else if (isColor(token))
                    borderColor = color(token)
                else if (isBorderStyle(token))
                    borderStyle = makeBorderStyle(token)
                else
                    throw Exception("Invalid border property: ${token}")
            }
        }

    var borderColor : Color?
        get() = getAttribute<Color>("border-color")
        set(value) {
            attributes["border-color"] = value.toString()
        }

    var borderRadius : LinearDimension
        get() = getAttribute<LinearDimension>("border-radius", 0.px)
        set(value) {
            attributes["border-radius"] = value.toString()
        }

    var borderBottomLeftRadius : LinearDimension
        get() = getAttribute<LinearDimension>("border-bottom-left-radius", 0.px)
        set(value) {
            attributes["border-bottom-left-radius"] = value.toString()
        }

    var borderBottomRightRadius : LinearDimension
        get() = getAttribute<LinearDimension>("border-bottom-right-radius", 0.px)
        set(value) {
            attributes["border-bottom-right-radius"] = value.toString()
        }

    var borderTopLeftRadius : LinearDimension
        get() = getAttribute<LinearDimension>("border-top-left-radius", 0.px)
        set(value) {
            attributes["border-top-left-radius"] = value.toString()
        }

    var borderTopRightRadius : LinearDimension
        get() = getAttribute<LinearDimension>("border-top-right-radius", 0.px)
        set(value) {
            attributes["border-top-right-radius"] = value.toString()
        }

    var borderStyle : BorderStyle?
        get() = getAttribute<BorderStyle>("border-style")
        set(value) {
            attributes["border-style"] = value.toString()
        }

    var borderWidth : LinearDimension
        get() = getAttribute<LinearDimension>("border-width", 0.px)
        set(value) {
            attributes["border-width"] = value.toString()
        }

    var clear : Clear?
        get() = getAttribute<Clear>("clear")
        set(value) {
            attributes["clear"] = value.toString()
        }

    var color : Color?
        get() = getAttribute<Color>("color")
        set(value) {
            attributes["color"] = value.toString()
        }

    var float : Float?
        get() = getAttribute<Float>("float")
        set(value) {
            attributes["float"] = value.toString()
        }

    var fontFamily : String?
        get() = getAttribute<String>("font-family")
        set(value) {
            attributes["font-family"] = value.toString()
        }

    var fontSize : LinearDimension?
        get() = getAttribute<LinearDimension>("font-size")
        set(value) {
            attributes["font-size"] = value.toString()
        }

    var fontWeight : FontWeight
        get() = getAttribute<FontWeight>("font-weight")
        set(value) {
            attributes["font-weight"] = value.toString()
        }

    var height : LinearDimension?
        get() = getAttribute<LinearDimension>("height")
        set(value) {
            attributes["height"] = value.toString()
        }

    var lineHeight : LinearDimension?
        get() = getAttribute<LinearDimension>("line-height")
        set(value) {
            attributes["line-height"] = value.toString()
        }

    var margin : BoxDimensions?
        get() = getAttribute<BoxDimensions>("margin")
        set(value) {
            attributes["margin"] = value.toString()
        }

    var marginTop : LinearDimension?
        get() = getAttribute<LinearDimension>("margin-top")
        set(value) {
            attributes["margin-top"] = value.toString()
        }

    var marginBottom : LinearDimension?
        get() = getAttribute<LinearDimension>("margin-bottom")
        set(value) {
            attributes["margin-bottom"] = value.toString()
        }

    var marginLeft : LinearDimension?
        get() = getAttribute<LinearDimension>("margin-left")
        set(value) {
            attributes["margin-left"] = value.toString()
        }

    var marginRight : LinearDimension?
        get() = getAttribute<LinearDimension>("margin-right")
        set(value) {
            attributes["margin-right"] = value.toString()
        }

    var maxHeight : LinearDimension?
        get() = getAttribute<LinearDimension>("max-height")
        set(value) {
            attributes["max-height"] = value.toString()
        }

    var maxWidth : LinearDimension?
        get() = getAttribute<LinearDimension>("max-width")
        set(value) {
            attributes["max-width"] = value.toString()
        }

    var minHeight : LinearDimension?
        get() = getAttribute<LinearDimension>("min-height")
        set(value) {
            attributes["min-height"] = value.toString()
        }

    var minWidth : LinearDimension?
        get() = getAttribute<LinearDimension>("min-width")
        set(value) {
            attributes["min-width"] = value.toString()
        }

    var overflow : Overflow
        get() = getAttribute<Overflow>("overflow", Overflow.inherit)
        set(value) {
            attributes["overflow"] = value.toString()
        }

    var padding : BoxDimensions?
        get() = getAttribute<BoxDimensions>("padding")
        set(value) {
            attributes["padding"] = value.toString()
        }

    var paddingTop : LinearDimension?
        get() = getAttribute<LinearDimension>("padding-top")
        set(value) {
            attributes["padding-top"] = value.toString()
        }

    var paddingBottom : LinearDimension?
        get() = getAttribute<LinearDimension>("padding-bottom")
        set(value) {
            attributes["padding-bottom"] = value.toString()
        }

    var paddingLeft : LinearDimension?
        get() = getAttribute<LinearDimension>("padding-left")
        set(value) {
            attributes["padding-left"] = value.toString()
        }

    var paddingRight : LinearDimension?
        get() = getAttribute<LinearDimension>("padding-right")
        set(value) {
            attributes["padding-right"] = value.toString()
        }

    var textAlign : TextAlign
        get() = getAttribute<TextAlign>("text-align", TextAlign.inherit)
        set(value) {
            attributes["text-align"] = value.toString()
        }

    var verticalAlign : VerticalAlign
        get() = getAttribute<VerticalAlign>("vertical-align", VerticalAlign.inherit)
        set(value) {
            attributes["vertical-align"] = value.toString()
        }

    var width : LinearDimension?
        get() = getAttribute<LinearDimension>("width")
        set(value) {
            attributes["width"] = value.toString()
        }
}