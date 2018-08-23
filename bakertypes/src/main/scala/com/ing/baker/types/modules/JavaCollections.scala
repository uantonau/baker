package com.ing.baker.types.modules

import java.lang.reflect.ParameterizedType
import java.util

import com.ing.baker.types.Converters.{getBaseClass, isAssignableToBaseClass}
import com.ing.baker.types.{ListType, ListValue, RecordValue, TypeConverter, TypeModule, Value}

import scala.collection.JavaConverters._

object JavaCollections {

  def getTypeParameter(javaType: java.lang.reflect.Type, index: Int): java.lang.reflect.Type = {
    javaType.asInstanceOf[ParameterizedType].getActualTypeArguments()(index)
  }

  class ListModule extends TypeModule {

    val baseClass = classOf[java.util.List[_]]

    override def isApplicable(javaType: java.lang.reflect.Type): Boolean =
      isAssignableToBaseClass(javaType, baseClass)

    override def readType(context: TypeConverter, javaType: java.lang.reflect.Type) = {
      val entryType = context.readType(getTypeParameter(javaType, 0))
      ListType(entryType)
    }

    override  def toJava(context: TypeConverter, value: Value, javaType: java.lang.reflect.Type) = value match {
      case ListValue(entries) if isApplicable(javaType) =>
        val entryType = getTypeParameter(javaType, 0)
        val list = new util.ArrayList[Any]()
        entries.foreach { e =>
          val value = context.toJava(e, entryType)
          list.add(value)
        }
        list
    }

    def fromJava(context: TypeConverter, obj: Any): Value = obj match {
      case list: java.util.List[_] => ListValue(list.asScala.toList.map(context.fromJava))
    }
  }

  class SetModule extends TypeModule {

    val baseClass = classOf[java.util.Set[_]]

    override def isApplicable(javaType: java.lang.reflect.Type): Boolean =
      isAssignableToBaseClass(javaType, baseClass)

    override def readType(context: TypeConverter, javaType: java.lang.reflect.Type) = {
      val entryType = context.readType(getTypeParameter(javaType, 0))
      ListType(entryType)
    }

    override  def toJava(context: TypeConverter, value: Value, javaType: java.lang.reflect.Type) = value match {
      case ListValue(entries) if isApplicable(javaType) =>
        val entryType = getTypeParameter(javaType, 0)
        val list = new util.HashSet[Any]()
        entries.foreach { e =>
          val value = context.toJava(e, entryType)
          list.add(value)
        }
        list
    }

    def fromJava(context: TypeConverter, obj: Any): Value = obj match {
      case set: java.util.Set[_] => ListValue(set.asScala.toList.map(context.fromJava))
    }
  }

  class MapModule extends TypeModule {

    val baseClass = classOf[java.util.Map[_,_]]

    override def isApplicable(javaType: java.lang.reflect.Type): Boolean =
      isAssignableToBaseClass(javaType, baseClass)

    override def readType(context: TypeConverter, javaType: java.lang.reflect.Type) = {
      val entryType = context.readType(getTypeParameter(javaType, 0))
      ListType(entryType)
    }

    override  def toJava(context: TypeConverter, value: Value, javaType: java.lang.reflect.Type) = value match {
      case RecordValue(entries) if classOf[java.util.Map[_,_]].isAssignableFrom(getBaseClass(javaType)) =>
        val keyType = getTypeParameter(javaType, 0)

        if (keyType != classOf[String])
          throw new IllegalArgumentException(s"Unsuported key type: $keyType")

        val valueType = getTypeParameter(javaType, 1)

        val javaMap: java.util.Map[String, Any] = new util.HashMap[String, Any]()

        entries.foreach { case (name, value) => javaMap.put(name, context.toJava(value, valueType)) }

        javaMap
    }

    def fromJava(context: TypeConverter, obj: Any): Value = obj match {
      case map: java.util.Map[_, _] =>
        val entries: Map[String, Value] = map.entrySet().iterator().asScala.map {
          e => e.getKey.asInstanceOf[String] -> context.fromJava(e.getValue)
        }.toMap
        RecordValue(entries)
    }
  }
}


