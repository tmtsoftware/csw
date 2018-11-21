package csw.command.client

/**
 * Store represents the storage of a set of values against a given key
 *
 * @param map initial store map
 * @tparam K type of the key
 * @tparam V type of the value
 */
case class Store[K, V](map: Map[K, Set[V]]) {

  /**
   * Add or update the value in the store against the given key
   *
   * @param key key of the store
   * @param value value against the key
   * @return updated store after adding/update value
   */
  def addOrUpdate(key: K, value: V): Store[K, V] = map.get(key) match {
    case Some(valuesPresent) => map.updated(key, valuesPresent + value)
    case None                => map.updated(key, Set(value))
  }

  /**
   * Remove the value from the store against the given key
   *
   * @param key key of the store
   * @param value value to be removed against the key
   * @return updated store after removing the value
   */
  def remove(key: K, value: V): Store[K, V] = map.get(key) match {
    case Some(valuesPresent) => map.updated(key, valuesPresent - value)
    case None                => map
  }

  /**
   * Remove the value from the store. The value could be present against multiple keys and it will be removed from all places.
   *
   * @param value value to be removed from the store
   * @return updated store after removing the value
   */
  def remove(value: V): Store[K, V] = {
    def removeValue(keys: List[K], store: Store[K, V]): Store[K, V] = {
      keys match {
        case Nil                  ⇒ store
        case key :: remainingKeys ⇒ removeValue(remainingKeys, remove(key, value))
      }
    }

    removeValue(map.keys.toList, this)
  }

  /**
   * Get the set of values against the given key. If the key is not present then empty set would be returned
   *
   * @param key get the values against the given key
   * @return set of values or empty set if key is not present
   */
  def get(key: K): Set[V] = map.getOrElse(key, Set.empty[V])
}

object Store {
  implicit def fromMap[K, V](map: Map[K, Set[V]]): Store[K, V] = new Store(map)
}
