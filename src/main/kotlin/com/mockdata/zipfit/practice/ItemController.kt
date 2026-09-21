package com.mockdata.zipfit.practice

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@Entity
class Item(
	var name: String,
	var price: Int,
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
)

interface ItemRepository : JpaRepository<Item, Long>

// PUT/POST: 모든 필드 필수
data class ItemRequest(
	@field:NotBlank val name: String,
	@field:PositiveOrZero val price: Int,
)

// PATCH: 보낸 필드만 수정 (null = 안 보냄)
data class ItemPatch(val name: String? = null, val price: Int? = null)

@RestController
@RequestMapping("/items")
class ItemController(private val repo: ItemRepository) {

	private fun find(id: Long) = repo.findById(id).orElseThrow {
		ResponseStatusException(HttpStatus.NOT_FOUND, "item $id not found")
	}

	@GetMapping
	fun list(): List<Item> = repo.findAll(Sort.by("id"))

	@GetMapping("/{id}")
	fun get(@PathVariable id: Long): Item = find(id)

	@PostMapping
	fun create(@Valid @RequestBody req: ItemRequest): ResponseEntity<Item> {
		val item = repo.save(Item(req.name, req.price))
		return ResponseEntity.created(URI.create("/items/${item.id}")).body(item) // 201 + Location
	}

	@PutMapping("/{id}")
	fun replace(@PathVariable id: Long, @Valid @RequestBody req: ItemRequest): Item =
		repo.save(find(id).apply { name = req.name; price = req.price })

	@PatchMapping("/{id}")
	fun patch(@PathVariable id: Long, @RequestBody req: ItemPatch): Item =
		repo.save(find(id).apply { req.name?.let { name = it }; req.price?.let { price = it } })

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@PathVariable id: Long) = repo.delete(find(id))
}
