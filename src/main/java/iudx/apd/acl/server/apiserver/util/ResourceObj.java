package iudx.apd.acl.server.apiserver.util;

import iudx.apd.acl.server.policy.util.ItemType;
import java.util.Objects;
import java.util.UUID;

/**
 * A class representing a resource object with item ID, provider ID, and resource group ID
 * (optional). This class is used to store information about a resource/resource_group.
 */
public class ResourceObj {
  private final UUID itemId;
  private final UUID providerId;
  private final UUID resourceGroupId;
  private final String resourceServerUrl;
  private final ItemType itemType;
  private final boolean isGroupLevelResource;
  private final String apdUrl;

  /**
   * Constructs a new ResourceObj with the given item ID, provider ID, and resource group ID. If the
   * item is resource group, the resource group ID will be null.
   *
   * @param itemId The unique ID of the resource item.
   * @param providerId The unique ID of the provider who owns the resource.
   * @param resourceGroupId The unique ID of the resource group to which the resource belongs (can
   *     be null).
   * @param resourceServerUrl The resource server URL to which the resource item belong.
   * @param isGroupLevelResource Boolean which is true when the resource is Rs-Group and vice-verse.
   */
  public ResourceObj(
      UUID itemId,
      UUID providerId,
      UUID resourceGroupId,
      String resourceServerUrl,
      boolean isGroupLevelResource,
      String apdUrl) {
    this.itemId = itemId;
    this.providerId = providerId;
    this.resourceGroupId = isGroupLevelResource ? null : resourceGroupId;
    this.resourceServerUrl = resourceServerUrl;
    this.isGroupLevelResource = isGroupLevelResource;
    this.itemType = isGroupLevelResource ? ItemType.RESOURCE_GROUP : ItemType.RESOURCE;
    this.apdUrl = apdUrl;
  }

  /**
   * Get the item ID of the resource/resource_group.
   *
   * @return The item ID as a UUID.
   */
  public UUID getItemId() {
    return itemId;
  }

  /**
   * Get the provider ID of the resource/resource_group.
   *
   * @return The provider ID as a UUID.
   */
  public UUID getProviderId() {
    return providerId;
  }

  /**
   * Get the resource group ID of the resource.
   *
   * @return The resource group ID as a UUID, or null if the item is resource group.
   */
  public UUID getResourceGroupId() {
    return resourceGroupId;
  }

  /**
   * Get the resource server URL of the resource.
   *
   * @return The resource server URL as a String.
   */
  public String getResourceServerUrl() {
    return resourceServerUrl;
  }

  /**
   * Tells if the resource is resource level or resource group level
   *
   * @return RESOURCE_GROUP, if the resource is resource group level, RESOURCE if the item is
   *     resource level
   */
  public ItemType getItemType() {
    return itemType;
  }

  /**
   * Tells if the resource is resource level or resource group level
   *
   * @return true, if the resource is resource group level, false if the item is resource level
   */
  public boolean getIsGroupLevelResource() {
    return isGroupLevelResource;
  }

  /**
   * get APD URL under which the resource is registered
   * */
  public String getApdUrl(){return apdUrl;}

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof ResourceObj)) return false;
    ResourceObj that = (ResourceObj) object;
    return isGroupLevelResource == that.isGroupLevelResource && Objects.equals(itemId, that.itemId) && Objects.equals(providerId, that.providerId) && Objects.equals(resourceGroupId, that.resourceGroupId) && Objects.equals(resourceServerUrl, that.resourceServerUrl) && itemType == that.itemType && Objects.equals(apdUrl, that.apdUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemId, providerId, resourceGroupId, resourceServerUrl, itemType, isGroupLevelResource, apdUrl);
  }
}
